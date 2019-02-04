package net.es.sense.rm.api;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.api.common.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception
 * HTTP Status Code
 * ConversionNotSupportedException	500 (Internal Server Error)
 * HttpMediaTypeNotAcceptableException	406 (Not Acceptable)
 * HttpMediaTypeNotSupportedException	415 (Unsupported MediaType)
 * HttpMessageNotReadableException	400 (Bad Request)
 * HttpMessageNotWritableException	500 (Internal Server Error)
 * HttpRequestMethodNotSupportedException	405 (Method Not Allowed)
 * MissingServletRequestParameterException	400 (Bad Request)
 * NoSuchRequestHandlingMethodException 404 (Not Found)
 * TypeMismatchException	400 (Bad Request)
 *
 * @author hacksaw
 */
@Slf4j
public abstract class SenseController {

  /**
   *
   * @param ex
   * @return
   */
  @ExceptionHandler({MalformedURLException.class, URISyntaxException.class, Exception.class})
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public ResponseEntity<?> handleMalformedUrlException(Exception ex) {
    log.error("handleMalformedUrlException: ", ex);
    Error error = Error.builder()
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .error_description(ex.getMessage())
            .build();
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(NoSuchElementException.class)
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  @ResponseBody
  public ResponseEntity<?> handleResourceNotFoundException(NoSuchElementException ex) {
    log.error("handleResourceNotFoundException: ", ex);
    Error error = Error.builder()
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .error_description(ex.getMessage())
            .build();
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }
}
