package com.ikelin;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An Apache Maven Wagon Provider for Amazon S3.
 */
public class S3Wagon extends StreamWagon {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3Wagon.class);

  private final AmazonS3 amazonS3;
  private final TransferManager transferManager;
  private final ConcurrentHashMap<Upload, OutputStream> uploads;

  /**
   * Creates a S3 Wagon using default {@code AmazonS3} and default {@code TransferManager}.
   */
  public S3Wagon() {
    this(AmazonS3ClientBuilder.defaultClient(), TransferManagerBuilder.defaultTransferManager(),
        new ConcurrentHashMap<>());
  }

  /**
   * Creates a S3 Wagon using the provided {@code AmazonS3}, {@code TransferManager}, and {@code
   * ConcurrentHashMap} of {@code Upload} and its {@code OutputStream} in progress.
   *
   * @param amazonS3 a {@code AmazonS3} used to download artifacts
   * @param transferManager a {@code TransferManager} used to upload artifacts
   * @param uploads a {@code ConcurrentHashMap} map that holds {@code Upload} and its {@code
   *     OutputStream}
   */
  protected S3Wagon(final AmazonS3 amazonS3, final TransferManager transferManager,
      final ConcurrentHashMap<Upload, OutputStream> uploads) {
    super();
    this.amazonS3 = amazonS3;
    this.transferManager = transferManager;
    this.uploads = uploads;
  }

  @Override
  public void fillInputData(final InputData inputData) throws ResourceDoesNotExistException {
    Resource resource = inputData.getResource();
    String bucket = getBucket();
    String key = getKey(resource);

    if (!amazonS3.doesObjectExist(bucket, key)) {
      throw new ResourceDoesNotExistException(
          "File at S3 bucket=" + bucket + " key=" + key + " does not exist");
    }

    S3Object s3Object = amazonS3.getObject(bucket, key);
    InputStream inputStream = new BufferedInputStream(s3Object.getObjectContent());
    inputData.setInputStream(inputStream);
    ObjectMetadata meta = s3Object.getObjectMetadata();
    resource.setContentLength(meta.getContentLength());
    resource.setLastModified(meta.getLastModified().getTime());
  }

  @Override
  public void fillOutputData(final OutputData outputData) throws TransferFailedException {
    Resource resource = outputData.getResource();
    String bucket = getBucket();
    String key = getKey(resource);

    // connect input stream to transfer manager to output data's output stream
    PipedInputStream inputStream = new PipedInputStream(AbstractWagon.DEFAULT_BUFFER_SIZE);
    PipedOutputStream outputStream;
    try {
      outputStream = new PipedOutputStream(inputStream);
    } catch (IOException ioe) {
      throw new TransferFailedException("Unable to put to S3 bucket=" + bucket + " key=" + key,
          ioe);
    }

    ObjectMetadata meta = new ObjectMetadata();
    meta.setContentLength(resource.getContentLength());

    PutObjectRequest request = new PutObjectRequest(bucket, key, inputStream, meta);
    Upload upload = transferManager.upload(request);
    uploads.put(upload, outputStream);

    outputData.setOutputStream(outputStream);
  }

  @Override
  protected void openConnectionInternal() {
  }

  /**
   * Waits for all {@code Upload} to finish and then closes all {@code OutputStream}.
   */
  @Override
  public void closeConnection() throws ConnectionException {
    Iterator<Upload> iterator = uploads.keySet().iterator();
    while (iterator.hasNext()) {
      // wait for upload to finish
      Upload upload = iterator.next();
      try {
        upload.waitForUploadResult();
      } catch (InterruptedException ie) {
        LOGGER.error("Failed to wait for upload to S3 to complete", ie);
        Thread.currentThread().interrupt();
      }

      // close output stream
      OutputStream outputStream = uploads.get(upload);
      try {
        outputStream.close();
      } catch (IOException ioe) {
        throw new ConnectionException(ioe.getMessage(), ioe);
      }
      iterator.remove();
    }
  }

  private String getBucket() {
    return getRepository().getHost();
  }

  private String getKey(final Resource resource) {
    return getRepository().getBasedir().substring(1) + "/" + resource.getName();
  }

}
