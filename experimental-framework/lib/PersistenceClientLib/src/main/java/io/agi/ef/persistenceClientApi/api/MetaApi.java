package io.agi.ef.persistenceClientApi.api;

import io.agi.ef.persistenceClientApi.ApiException;
import io.agi.ef.persistenceClientApi.ApiClient;
import io.agi.ef.persistenceClientApi.Configuration;
import io.agi.ef.persistenceClientApi.Pair;
import io.agi.ef.persistenceClientApi.TypeRef;


import java.util.*;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2015-11-02T17:23:34.998+11:00")
public class MetaApi {
  private ApiClient apiClient;

  public MetaApi() {
    this(Configuration.getDefaultApiClient());
  }

  public MetaApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  
  /**
   * Retrieves a list of tables and views
   * The list of accessible tables and views is returned.
   * @return void
   */
  public void rootGet () throws ApiException {
    Object postBody = null;
    byte[] postBinaryBody = null;
    
    // create path and map variables
    String path = "/".replaceAll("\\{format\\}","json");

    // query params
    List<Pair> queryParams = new ArrayList<Pair>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, Object> formParams = new HashMap<String, Object>();

    

    

    

    final String[] accepts = {
      "application/json"
    };
    final String accept = apiClient.selectHeaderAccept(accepts);

    final String[] contentTypes = {
      
    };
    final String contentType = apiClient.selectHeaderContentType(contentTypes);

    String[] authNames = new String[] {  };

    

    
    
    apiClient.invokeAPI(path, "GET", queryParams, postBody, postBinaryBody, headerParams, formParams, accept, contentType, authNames, null);
    
    


  }
  
}
