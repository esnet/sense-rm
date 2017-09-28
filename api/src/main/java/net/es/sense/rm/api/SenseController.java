/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.es.sense.rm.api;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.api.common.Error;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception	HTTP Status Code
 * ConversionNotSupportedException	500 (Internal Server Error)
 * HttpMediaTypeNotAcceptableException	406 (Not Acceptable)
 * HttpMediaTypeNotSupportedException	415 (Unsupported Media Type)
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
  @ExceptionHandler({ MalformedURLException.class, URISyntaxException.class, Exception.class })
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public Error handleMalformedUrlException(MalformedURLException ex) {
    log.error("handleMalformedUrlException: ", ex);
    return new Error(HttpStatus.INTERNAL_SERVER_ERROR.toString(), ex.getLocalizedMessage(), null);
  }

  @ExceptionHandler(NoSuchElementException.class)
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  public Error handleResourceNotFoundException(NoSuchElementException ex) {
    log.error("handleResourceNotFoundException: ", ex);
    return new Error(HttpStatus.NOT_FOUND.toString(), ex.getLocalizedMessage(), null);
  }
}
