package net.es.sense.rm.api.common;

import jakarta.ws.rs.core.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.HttpURLConnection;

/**
 *
 * @author hacksaw
 */
public class HttpConstants {

  // Request header parameters.
  public static final String ACCEPT_NAME = "Accept";
  public static final String ACCEPT_MSG =
          "Provides media types that are acceptable for the response. At the moment "
          + MediaType.APPLICATION_JSON_VALUE + " is the supported response encoding.";

  public static final String IF_MODIFIED_SINCE_NAME = "If-Modified-Since";
  public static final String IF_MODIFIED_SINCE_MSG =
          "The HTTP request may contain the If-Modified-Since header requesting all models with "
          + "creationTime after the specified date. The date must be specified in RFC 1123 format.";
  public static final String IF_MODIFIED_SINCE_DEFAULT = "Thu, 02 Jan 1970 00:00:00 GMT";

  public static final String ETAG_NAME = "ETag";
  public static final String ETAG_MSG =
          "The HTTP request may contain the If-Modified-Since header requesting all models with "
          + "creationTime after the specified date. The date must be specified in RFC 1123 format.";

  public static final String IF_NONE_MATCH_NAME = "If-None-Match";
  public static final String IF_NONE_MATCH_MSG =
          "The HTTP request may contain the If-None-Match header specifying a previously provided "
          + "resource ETag value.  If the resource version identified by the provided ETag value "
          + "has not changed then a 304 NOT_MODIFIED is returned, otherwise a new version of the "
          + "resource is returned.";

  // Response header parameters.
  public static final String CONTENT_TYPE_NAME = "Content-Type";
  public static final String CONTENT_TYPE_DESC =
          "Provides media type used to encode the result of the operation based on those values "
          + "provided in the Accept request header. At the moment application/json is the only "
          + "supported Content-Type encoding.";

  public static final String LAST_MODIFIED_NAME = "Last-Modified";
  public static final String LAST_MODIFIED_DESC =
          "The HTTP response should contain the Last-Modified header with the date set to the "
          + "RFC 1123 format of the newest model version's creationTime.";

  public static final String CONTENT_LOCATION_NAME = HttpHeaders.CONTENT_LOCATION;
  public static final String CONTENT_LOCATION_DESC =
          "The HTTP Content-Location header is an entity-header that gives another location for "
          + " the data that is returned and also tells how to access the resource by indicating "
          + "the direct URL.";

  // Query parameters.
  public static final String CURRENT_NAME = "current";
  public static final String CURRENT_MSG =
          "If current=true then a collection of models containing only the most recent model "
          + "will be returned. Default value is current=false.";

  public static final String SUMMARY_NAME = "summary";
  public static final String SUMMARY_MSG =
          "If summary=true then a summary collection of models will be returned including the "
          + "model meta-data while excluding the model element. Default value is summary=false.";

  public static final String MODEL_NAME = "model";
  public static final String MODEL_MSG =
          "If model=turtle then the returned model element will contain the full topology model "
          + "in a TURTLE representation. Default value is model=turtle.";
  public static final String MODEL_TURTLE = "turtle";

  public static final String ENCODE_NAME = "encode";
  public static final String ENCODE_MSG =
          "If encode=true then the embedded topology model will be transfer encoded using gzip "
          + "(contentType=\"application/x-gzip\") and base64 encoding (contentTransferEncoding=\"base64\").  "
          + "This will reduce the transfer size and encapsulate the original model contents.  Default "
          + "value is encode=false.";

  // Path parameters.
  public static final String ID_NAME = "id";
  public static final String ID_MSG = "The UUID uniquely identifying the topology model resource.";

  public static final String DELTAID_NAME = "deltaId";
  public static final String DELTAID_MSG = "The UUID uniquely identifying the delta resource.";

  // HTTP Response Codes/
  public static final String OK_CODE = "" + HttpURLConnection.HTTP_OK;
  public static final String OK_MSG = "OK - Success";

  public static final String OK_TOPOLOGIES_MSG =
          "OK - On success, a JSON structure-containing (a possibly empty) collection of "
          + "topology model versions. Each model resource entry will contain id, creationTime, "
          + "href, and model elements (summary=true will exclude the model element).";
  public static final String OK_TOPOLOGY_MSG =
          "OK - On success, a JSON structure-containing a specific topology model version. "
          + "The topology model resource entry will contain id, creationTime, "
          + "href, and model elements (summary=true will exclude the model element).";

  public static final String OK_DELTAS_MSG =
          "OK - On success a JSON structure-containing (a possibly empty) collection of delta "
          + "resources. Each delta resource entry will contain the delta id, creationTime, "
          + "href, modelId, state, reduction, addition, and result elements (summary=true will "
          + "exclude the reduction, addition, and result elements).";
  public static final String OK_DELTA_MSG =
          "OK - On success, a JSON structure-containing (a possibly empty) delta resource. A "
          + "delta resource will contain id, lastModified, href, modelId, state, reduction, "
          + "addition, and result elements.";
  public static final String OK_DELTA_COUNTER_MSG =
          "OK - A 200 OK indicates the Resource Manager is unwilling to complete the requested "
          + "model delta but is suggesting a possible alternative delta in the returned JSON "
          + "structure.";

  public static final String OK_LOGS_MSG =
            "OK - On success, a JSON structure-containing (a possibly empty) collection of "
          + "operational log messages. Each log resource entry will contain a unique uuid,"
          + " a resource id to which the log applies (model, delta, etc.), creationTime, ???";

  public static final String OK_MEASURE_MSG =
          "OK - On success, a JSON structure-containing (a possibly empty) collection of "
                  + "operational log messages. Each log resource entry will contain a unique uuid,"
                  + " a resource id to which the log applies (model, delta, etc.), creationTime, ???";

  public static final String CREATED_CODE = "" + HttpURLConnection.HTTP_CREATED;
  public static final String CREATED_MSG =
          "Created - Indicates the Resource Manager is willing to complete the requested model "
          + "delta, and has created a delta resource to track the agreed change. A JSON "
          + "structure containing the newly created delta resource will be returned tracking the "
          + "agreed to changes, and the resulting topology model.";

  public static final String NO_CONTENT_CODE = "" + HttpURLConnection.HTTP_NO_CONTENT;
  public static final String NO_CONTENT_MSG =
          "Commited - Indicates the Resource Manager has committed the requested model delta.";

  public static final String NOT_MODIFIED = "" + HttpURLConnection.HTTP_NOT_MODIFIED;
  public static final String NOT_MODIFIED_MSG =
          "Not Modified - A query using the If-Modified-Since header found no reources matching "
          + "the query. The Last-Modified header will be returned containing the date of last "
          + "modification.";

  public static final String BAD_REQUEST_CODE = "" + HttpURLConnection.HTTP_BAD_REQUEST;
  public static final String BAD_REQUEST_MSG =
          "Bad Request - The server due to malformed syntax or invalid query parameters could "
          + "not understand the client’s request.";

  public static final String UNAUTHORIZED_CODE = "" + HttpURLConnection.HTTP_UNAUTHORIZED;
  public static final String UNAUTHORIZED_MSG = "Unauthorized";

  public static final String FORBIDDEN_CODE = "" + HttpURLConnection.HTTP_FORBIDDEN;
  public static final String FORBIDDEN_MSG =
          "Forbidden - Requester is not authorized to access the requested resource.";

  public static final String NOT_FOUND_CODE = "" + HttpURLConnection.HTTP_NOT_FOUND;
  public static final String NOT_FOUND_MSG =
          "Not Found - The provider is not currently capable of serving resource models.";

  public static final String NOT_ACCEPTABLE_CODE = "" + HttpURLConnection.HTTP_NOT_ACCEPTABLE;
  public static final String NOT_ACCEPTABLE_MSG =
          "Not Acceptable - The requested resource is capable of generating only content not "
          + "acceptable according to the Accept headers sent in the request.";

  public static final String CONFLICT_CODE = "" + HttpURLConnection.HTTP_CONFLICT;
  public static final String CONFLICT_MSG =
          "Conflict - The request could not be completed due to a conflict with the current "
          + " state of the resource (model).";

  public static final String INTERNAL_ERROR_CODE = "" + HttpURLConnection.HTTP_INTERNAL_ERROR;
  public static final String INTERNAL_ERROR_MSG =
          "Internal Server Error - A generic error message given when an unexpected condition "
          + "was encountered and a more specific message is not available.";

}
