package com.ikelin;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@RunWith(MockitoJUnitRunner.class)
public class S3WagonTest {

  private S3Wagon s3Wagon;

  @Mock
  private AmazonS3 amazonS3;

  @Mock
  private TransferManager transferManager;

  @Mock
  private Repository repository;

  @Mock
  private Resource resource;

  private ConcurrentHashMap<Upload, OutputStream> uploads;
  private long contentLength;
  private String bucket;
  private String key;

  @Before
  public void before() {
    uploads = new ConcurrentHashMap<>();

    s3Wagon = spy(new S3Wagon(amazonS3, transferManager, uploads));
    doReturn(repository).when(s3Wagon).getRepository();

    resource = mock(Resource.class);
    doReturn("key").when(resource).getName();

    contentLength = 1024L;
    doReturn(contentLength).when(resource).getContentLength();

    bucket = "bucket";
    doReturn(bucket).when(repository).getHost();
    doReturn("/path/to").when(repository).getBasedir();

    key = "path/to/key";
  }

  @Test(expected = ResourceDoesNotExistException.class)
  public void givenFillInputData_whenS3ObjectDoesNotExist_thenThrowResourceDoesNotExistException()
      throws ResourceDoesNotExistException {
    InputData inputData = mock(InputData.class);
    doReturn(resource).when(inputData).getResource();

    doReturn(false).when(amazonS3).doesObjectExist(bucket, key);

    s3Wagon.openConnectionInternal();
    s3Wagon.fillInputData(inputData);
  }

  @Test
  public void givenFillInputData_whenS3ObectExists_thenInputStreamIsFilled()
      throws ResourceDoesNotExistException {
    InputData inputData = mock(InputData.class);
    doReturn(resource).when(inputData).getResource();

    doReturn(true).when(amazonS3).doesObjectExist(bucket, key);

    S3Object s3Object = mock(S3Object.class);
    doReturn(s3Object).when(amazonS3).getObject(bucket, key);

    S3ObjectInputStream s3ObjectInputStream = mock(S3ObjectInputStream.class);
    doReturn(s3ObjectInputStream).when(s3Object).getObjectContent();

    ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
    doReturn(objectMetadata).when(s3Object).getObjectMetadata();

    long contentLength = 1024;
    doReturn(contentLength).when(objectMetadata).getContentLength();

    Date lastModified = new Date();
    doReturn(lastModified).when(objectMetadata).getLastModified();

    s3Wagon.openConnectionInternal();
    s3Wagon.fillInputData(inputData);

    verify(inputData).setInputStream(any(BufferedInputStream.class));
    verify(resource).setContentLength(contentLength);
    verify(resource).setLastModified(lastModified.getTime());
  }

  @Test
  public void givenFillOutputData_whenOutputStreamIsValid_thenOutputStreamIsFilled()
      throws TransferFailedException {
    OutputData outputData = mock(OutputData.class);
    doReturn(resource).when(outputData).getResource();

    Upload upload = mock(Upload.class);
    doReturn(upload).when(transferManager).upload(any(PutObjectRequest.class));

    s3Wagon.openConnectionInternal();
    s3Wagon.fillOutputData(outputData);

    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(transferManager).upload(captor.capture());

    PutObjectRequest putObjectRequest = captor.getValue();
    assertEquals(bucket, putObjectRequest.getBucketName());
    assertEquals(key, putObjectRequest.getKey());
    assertEquals(contentLength, putObjectRequest.getMetadata().getContentLength());

    assertEquals(1, uploads.size());

    verify(outputData).setOutputStream(any(PipedOutputStream.class));
  }

  @Test
  public void testFillOutputDataCloseConnection()
      throws TransferFailedException, ConnectionException, InterruptedException {
    OutputData outputData = mock(OutputData.class);
    doReturn(resource).when(outputData).getResource();

    Upload upload = mock(Upload.class);
    doReturn(upload).when(transferManager).upload(any(PutObjectRequest.class));

    assertEquals(0, uploads.size());

    s3Wagon.openConnectionInternal();
    s3Wagon.fillOutputData(outputData);

    assertEquals(1, uploads.size());

    s3Wagon.closeConnection();

    verify(upload).waitForUploadResult();
    assertEquals(0, uploads.size());
  }

  @Test
  public void givenCloseConnection_whenUploadInterruptedException_thenContinue()
      throws TransferFailedException, ConnectionException, InterruptedException {
    OutputData outputData = mock(OutputData.class);
    doReturn(resource).when(outputData).getResource();

    Upload upload = mock(Upload.class);
    doReturn(upload).when(transferManager).upload(any(PutObjectRequest.class));
    doThrow(InterruptedException.class).when(upload).waitForUploadResult();

    s3Wagon.openConnectionInternal();
    s3Wagon.fillOutputData(outputData);

    assertEquals(1, uploads.size());

    s3Wagon.closeConnection();

    verify(upload).waitForUploadResult();
    assertEquals(0, uploads.size());
  }

  @Test(expected = ConnectionException.class)
  public void givenCloseConnection_whenOutputStreamThrowsIOException_thenThrowsConnectionException()
      throws TransferFailedException, ConnectionException, IOException {
    OutputData outputData = mock(OutputData.class);
    doReturn(resource).when(outputData).getResource();

    Upload upload = mock(Upload.class);
    doReturn(upload).when(transferManager).upload(any(PutObjectRequest.class));

    s3Wagon.openConnectionInternal();
    s3Wagon.fillOutputData(outputData);

    OutputStream outputStream = mock(OutputStream.class);
    doThrow(IOException.class).when(outputStream).close();
    uploads.put(upload, outputStream);

    s3Wagon.closeConnection();
  }

}