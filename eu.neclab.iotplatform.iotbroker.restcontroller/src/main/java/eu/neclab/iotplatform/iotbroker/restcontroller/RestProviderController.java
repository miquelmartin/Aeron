/*******************************************************************************
 *   Copyright (c) 2014, NEC Europe Ltd.
 *   All rights reserved.
 *   
 *   Authors:
 *           * Salvatore Longo - salvatore.longo@neclab.eu
 *           * Tobias Jacobs - tobias.jacobs@neclab.eu
 *           * Raihan Ul-Islam - raihan.ul-islam@neclab.eu
 *  
 *    Redistribution and use in source and binary forms, with or without
 *    modification, are permitted provided that the following conditions are met:
 *   1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   3. All advertising materials mentioning features or use of this software
 *     must display the following acknowledgement:
 *     This product includes software developed by NEC Europe Ltd.
 *   4. Neither the name of the NEC nor the
 *     names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY NEC ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL NEC BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED 
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package eu.neclab.iotplatform.iotbroker.restcontroller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.neclab.iotplatform.iotbroker.commons.XmlValidator;
import eu.neclab.iotplatform.iotbroker.restcontroller.sanitycheck.SanityCheck;
import eu.neclab.iotplatform.ngsi.api.datamodel.AppendContextElementRequest;
import eu.neclab.iotplatform.ngsi.api.datamodel.AppendContextElementResponse;
import eu.neclab.iotplatform.ngsi.api.datamodel.Code;
import eu.neclab.iotplatform.ngsi.api.datamodel.ContextAttribute;
import eu.neclab.iotplatform.ngsi.api.datamodel.ContextAttributeResponse;
import eu.neclab.iotplatform.ngsi.api.datamodel.ContextElement;
import eu.neclab.iotplatform.ngsi.api.datamodel.ContextElementResponse;
import eu.neclab.iotplatform.ngsi.api.datamodel.Converter;
import eu.neclab.iotplatform.ngsi.api.datamodel.EntityId;
import eu.neclab.iotplatform.ngsi.api.datamodel.NotifyContextAvailabilityRequest;
import eu.neclab.iotplatform.ngsi.api.datamodel.NotifyContextAvailabilityResponse;
import eu.neclab.iotplatform.ngsi.api.datamodel.NotifyContextRequest;
import eu.neclab.iotplatform.ngsi.api.datamodel.NotifyContextResponse;
import eu.neclab.iotplatform.ngsi.api.datamodel.QueryContextRequest;
import eu.neclab.iotplatform.ngsi.api.datamodel.QueryContextResponse;
import eu.neclab.iotplatform.ngsi.api.datamodel.ReasonPhrase;
import eu.neclab.iotplatform.ngsi.api.datamodel.StatusCode;
import eu.neclab.iotplatform.ngsi.api.datamodel.SubscribeContextRequest;
import eu.neclab.iotplatform.ngsi.api.datamodel.SubscribeContextResponse;
import eu.neclab.iotplatform.ngsi.api.datamodel.SubscribeError;
import eu.neclab.iotplatform.ngsi.api.datamodel.UnsubscribeContextRequest;
import eu.neclab.iotplatform.ngsi.api.datamodel.UnsubscribeContextResponse;
import eu.neclab.iotplatform.ngsi.api.datamodel.UpdateActionType;
import eu.neclab.iotplatform.ngsi.api.datamodel.UpdateContextAttributeRequest;
import eu.neclab.iotplatform.ngsi.api.datamodel.UpdateContextElementRequest;
import eu.neclab.iotplatform.ngsi.api.datamodel.UpdateContextElementResponse;
import eu.neclab.iotplatform.ngsi.api.datamodel.UpdateContextRequest;
import eu.neclab.iotplatform.ngsi.api.datamodel.UpdateContextResponse;
import eu.neclab.iotplatform.ngsi.api.datamodel.UpdateContextSubscriptionRequest;
import eu.neclab.iotplatform.ngsi.api.datamodel.UpdateContextSubscriptionResponse;
import eu.neclab.iotplatform.ngsi.api.ngsi10.Ngsi10Interface;
import eu.neclab.iotplatform.ngsi.api.ngsi9.Ngsi9Interface;


/**
 *  This class implements the RESTful binding of NGSI 10 defined by the
 *  FI-WARE project. It maps operations on the RESTful interface to operations
 *  on an NGSI java interface.
 *  <p>
 *  In addition, also the NGSI 9 method for receiving notifications is
 *  implemented by this class.
 *
 */
@Controller
public class RestProviderController {

	/** The logger. */
	private static Logger logger = Logger
			.getLogger(RestProviderController.class);

	/** String representing json content type. */
	private final String CONTENT_TYPE_JSON = "application/json";

	/** String representing xml content type. */
	private final String CONTENT_TYPE_XML = "application/xml";

	/** The component for receiving NGSI9 requests. */
	@Autowired
	private Ngsi9Interface ngsi9Core;

	/** The component for receiving NGSI 10 requests. */
	private Ngsi10Interface ngsiCore;

	/**
	 * Returns a pointer to the component which receives
	 * NGSI 10 requests arriving at the controller.
	 *
	 * @return the ngsi core
	 */
	public Ngsi10Interface getNgsiCore() {
		return ngsiCore;
	}

	/**
	 * Assigns a pointer to the component which will receive
	 * NGSI 10 requests arriving at the controller.
	 *
	 * @param ngsiCore
	 *            the new ngsi core
	 */
	public void setNgsiCore(Ngsi10Interface ngsiCore) {
		this.ngsiCore = ngsiCore;
	}

	/**
	 * Returns a pointer to the component which receives
	 * NGSI 9 requests arriving at the controller.
	 *
	 * @return the ngsi9 core
	 */
	public Ngsi9Interface getNgsi9Core() {
		return ngsi9Core;
	}

	/**
	 * Assigns a pointer to the component which will receive
	 * NGSI 9 requests arriving at the controller.
	 *
	 * @param ngsi9Core
	 *            the new ngsi9 core
	 */
	public void setNgsi9Core(Ngsi9Interface ngsi9Core) {
		this.ngsi9Core = ngsi9Core;
	}

	/** String representing the xml schema for NGSI 10. */
	private @Value("${schema_ngsi10_operation}")
	String sNgsi10schema;

	/** String representing the xml schema for NGSI 9. */
	private @Value("${schema_ngsi9_operation}")
	String sNgsi9schema;

	/** The validator for incoming message bodies. */
	private static XmlValidator validator = new XmlValidator();

	/**
	 * Instantiates a new controller object.
	 */
	public RestProviderController() {

	}

	/**
	 * The IoT Broker uses a fixed mapping for assigning
	 * NGSI status codes to HTTP status codes. This mapping
	 * is implemented by this private method.
	 *
	 * @param statusCode
	 *            the statusCode
	 * @return the http status
	 */
	private static HttpStatus makeHttpStatus(StatusCode statusCode) {

		if (statusCode == null) {
			return HttpStatus.OK;
		}

		int nCode = statusCode.getCode();

		if (nCode == Code.INTERNALERROR_500.getCode()) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}

		if (nCode >= Code.BADREQUEST_400.getCode()
				&& nCode < Code.INTERNALERROR_500.getCode()) {
			return HttpStatus.NOT_FOUND;
		}

		return HttpStatus.OK;

	}

	/**
	 * Redirector to the index.html web page.
	 *
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String index() {

		return "redirect:index.html";
	}

	/**
	 * Redirector to the index.html web page.
	 *
	 */
	@RequestMapping(value = "/index.html", method = RequestMethod.GET)
	public String operation() {

		return "index";
	}

	/**
	 * Redirector to the administration web page.
	 *
	 */
	@RequestMapping(value = "/monitoring", method = RequestMethod.GET)
	public String monitoring() {
		return "redirect:admin";
	}

	/**
	 * Redirector to the administration web page.
	 *
	 */
	@RequestMapping(value = "/admin", method = RequestMethod.GET)
	public String admin() {
		return "redirect:admin.html";
	}

	/**
	 * Redirector to the administration web page.
	 *
	 */
	@RequestMapping(value = "/admin.html", method = RequestMethod.GET)
	public String adminHtml() {
		return "admin";
	}

	/**
	 * Redirector to the "login failed" web page.
	 */
	@RequestMapping(value = "/loginfailed.html", method = RequestMethod.GET)
	public String logoutHtml() {
		return "loginfailed";
	}

	/**
	 * Executes the Sanity Check Procedure of the IoT Broker.
	 *
	 * @return the response entity
	 */
	@RequestMapping(value = "/ngsi10/sanityCheck", method = RequestMethod.GET, consumes = { "*/*" }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<SanityCheck> sanityCheck() {

		BundleContext bc = FrameworkUtil
				.getBundle(RestProviderController.class).getBundleContext();

		SanityCheck response = new SanityCheck("IoT Broker GE", "Sanity Check",
				"Version: " + bc.getBundle().getVersion());

		return new ResponseEntity<SanityCheck>(response, HttpStatus.OK);

	}

	/**
	 * Executes the standard NGSI 10 QueryContext method.
	 *
	 * @param requester
	 *            Represents the request message body and header. 
	 * @param request
	 *           The request body.
	 * @return The response body.
	 *
	 */
	@RequestMapping(value = "/ngsi10/queryContext", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<QueryContextResponse> complexQuery(
			HttpServletRequest requester,
			@RequestBody QueryContextRequest request) {

		logger.info(" <--- NGSI-10 has received request for Context query resource ---> \n");

		boolean status = validator.xmlValidation(request, sNgsi10schema);

		logger.debug("STATUS XML VALIDATOR" + status);


		if (!status) {

			QueryContextResponse response = ngsiCore.queryContext(request);

			logger.debug("Entity TYPE = "
					+ request.getEntityIdList().get(0).getType());

			return new ResponseEntity<QueryContextResponse>(response,
					makeHttpStatus(response.getErrorCode()));
		} else {

			QueryContextResponse response = new QueryContextResponse();
			StatusCode statusCode = new StatusCode(
					Code.BADREQUEST_400.getCode(),
					ReasonPhrase.BADREQUEST_400.toString(), "XML syntax Error!");

			response.setErrorCode(statusCode);

			return new ResponseEntity<QueryContextResponse>(response,
					HttpStatus.BAD_REQUEST);

		}

	}

	/**
	 * Executes the standard NGSI 10 SubscribeContext method.
	 *
	 * @param request
	 *            The request body.
	 * @return The response body.
	 */

	@RequestMapping(value = "/ngsi10/subscribeContext", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<SubscribeContextResponse> subscription(
			@RequestBody SubscribeContextRequest request) {

		logger.info(" <--- NGSI-10 has received request for Subscribe Context resource ---> \n");

		if (validator.xmlValidation(request, sNgsi10schema)) {
			SubscribeContextResponse response = new SubscribeContextResponse(
					null, new SubscribeError(null, new StatusCode(
							Code.BADREQUEST_400.getCode(),
							ReasonPhrase.BADREQUEST_400.toString(),
							"XML syntax Error!")));

			return new ResponseEntity<SubscribeContextResponse>(response,
					HttpStatus.BAD_REQUEST);
		}

		SubscribeContextResponse response = ngsiCore.subscribeContext(request);

		if (response.getSubscribeError() != null) {
			return new ResponseEntity<SubscribeContextResponse>(
					response,
					makeHttpStatus(response.getSubscribeError().getStatusCode()));
		}

		return new ResponseEntity<SubscribeContextResponse>(response,
				HttpStatus.OK);

	}

	/**
	 * Executes the standard NGSI 10 UpdateContextSubscription method.
	 *
	 * @param request
	 *            The request body.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/updateContextSubscription", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<UpdateContextSubscriptionResponse> updateContextSubscription(
			@RequestBody UpdateContextSubscriptionRequest request) {

		logger.info(" <--- NGSI-10 has received request for Update Context Subscription resource ---> \n");

		if (validator.xmlValidation(request, sNgsi10schema)) {
			UpdateContextSubscriptionResponse response = new UpdateContextSubscriptionResponse(
					null, new SubscribeError(null, new StatusCode(
							Code.BADREQUEST_400.getCode(),
							ReasonPhrase.BADREQUEST_400.toString(),
							"XML syntax Error!")));

			return new ResponseEntity<UpdateContextSubscriptionResponse>(
					response, HttpStatus.BAD_REQUEST);
		}

		UpdateContextSubscriptionResponse response = ngsiCore
				.updateContextSubscription(request);

		if (response.getSubscribeError() != null) {
			return new ResponseEntity<UpdateContextSubscriptionResponse>(
					response, makeHttpStatus(response.getSubscribeError()
							.getStatusCode()));
		}

		return new ResponseEntity<UpdateContextSubscriptionResponse>(response,
				HttpStatus.OK);
	}

	/**
	 * Executes the standard NGSI 10 UnsubscribeContext method.
	 *
	 * @param request
	 *            The request body.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/unsubscribeContext", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<UnsubscribeContextResponse> unsubscribeContext(
			@RequestBody UnsubscribeContextRequest request) {

		logger.info(" <--- NGSI-10 has received request for Unsubscribe context resource ---> \n");

		boolean status = validator.xmlValidation(request, sNgsi10schema);

		if (!status) {

			logger.info("SubscriptionIDRequest: " + request.getSubscriptionId());

			UnsubscribeContextResponse response = ngsiCore
					.unsubscribeContext(request);

			return new ResponseEntity<UnsubscribeContextResponse>(response,
					makeHttpStatus(response.getStatusCode()));

		} else {

			UnsubscribeContextResponse response = new UnsubscribeContextResponse();
			StatusCode statusCode = new StatusCode(
					Code.BADREQUEST_400.getCode(),
					ReasonPhrase.BADREQUEST_400.toString(), "XML sintax Error!");
			response.setStatusCode(statusCode);

			return new ResponseEntity<UnsubscribeContextResponse>(response,
					HttpStatus.BAD_REQUEST);

		}

	}

	/**
	 * Executes the standard NGSI 10 UpdateContext method.
	 *
	 * @param requester
	 *            Represents the HTTP request message.
	 * @param request
	 *            The request body.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/updateContext", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<UpdateContextResponse> updateContextResponse(
			HttpServletRequest requester,
			@RequestBody UpdateContextRequest request) {

		logger.info(" <--- NGSI-10 has received request for Update Context resource ---> \n");

		boolean status = validator.xmlValidation(request, sNgsi10schema);

		if (!status) {

			UpdateContextResponse response = ngsiCore.updateContext(request);

			return new ResponseEntity<UpdateContextResponse>(response,
					makeHttpStatus(response.getErrorCode()));

		} else {

			UpdateContextResponse response = new UpdateContextResponse(
					new StatusCode(Code.BADREQUEST_400.getCode(),
							ReasonPhrase.BADREQUEST_400.toString(),
							"XML syntax Error!"), null);

			return new ResponseEntity<UpdateContextResponse>(response,
					makeHttpStatus(response.getErrorCode()));
		}

	}





	/**
	 * Executes the convenience method for querying an individual
	 * context entity.
	 *
	 *
	 * @param id
	 *          The id of the context entity to query.
	 * @return The response body.
	 */

	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}", method = RequestMethod.GET, consumes = { "*/*" }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<ContextElementResponse> simpleQueryIdGet(
			@PathVariable("entityID") String id) {

		logger.info("<--- NGSI-10 has received request for Query ( GET ) to Individual context entity ---> \n");

		List<EntityId> entityIdList = new ArrayList<EntityId>();

		EntityId entity = new EntityId(id, null, false);
		entityIdList.add(entity);

		QueryContextRequest request = new QueryContextRequest(entityIdList,
				null, null);

		QueryContextResponse response = ngsiCore.queryContext(request);

		if (response.getErrorCode() != null) {
			if (response.getErrorCode().getCode() == Code.CONTEXTELEMENTNOTFOUND_404
					.getCode()) {

				ContextElementResponse contextElementResp = new ContextElementResponse(
						new ContextElement(new EntityId(id, null, false), null,
								null, null), response.getErrorCode());

				return new ResponseEntity<ContextElementResponse>(
						contextElementResp,
						makeHttpStatus(response.getErrorCode()));

			} else if (response.getErrorCode().getCode() == Code.INTERNALERROR_500
					.getCode()) {

				ContextElementResponse contextElementResp = new ContextElementResponse(
						new ContextElement(new EntityId(id, null, false), null,
								null, null), response.getErrorCode());

				return new ResponseEntity<ContextElementResponse>(
						contextElementResp,
						makeHttpStatus(response.getErrorCode()));

			}

		}

		ContextElementResponse contextElementResp = response
				.getListContextElementResponse().get(0);

		return new ResponseEntity<ContextElementResponse>(contextElementResp,
				makeHttpStatus(response.getErrorCode()));

	}

	/**
	 * Executes the convenience method for updating an individual
	 * context entity.
	 *
	 * @param EntityID
	 *            The id of the Context Entity to update.
	 * @param request
	 *            The request body.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}", method = RequestMethod.PUT, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<UpdateContextElementResponse> simpleQueryIdPut(
			@PathVariable("entityID") String EntityID,
			@RequestBody UpdateContextElementRequest request) {

		logger.info(" <--- NGSI-10 has received request for Update ( PUT ) to Individual context entity ---> \n");

		boolean status = validator.xmlValidation(request, sNgsi10schema);

		if (!status) {

			List<ContextElement> contextElementList = new ArrayList<ContextElement>();

			ContextElement contextElement = new ContextElement(new EntityId(
					EntityID, null, false), request.getAttributeDomainName(),
					request.getContextAttributeList(),
					request.getDomainMetadata());

			contextElementList.add(contextElement);
			UpdateContextRequest reqUpdate = new UpdateContextRequest(
					contextElementList, UpdateActionType.UPDATE);

			UpdateContextResponse response = ngsiCore.updateContext(reqUpdate);

			ContextAttributeResponse contextAttributeResp = new ContextAttributeResponse(
					response.getContextElementResponse().get(0)
							.getContextElement().getContextAttributeList(),
					response.getContextElementResponse().get(0).getStatusCode());
			UpdateContextElementResponse respUpdate = new UpdateContextElementResponse(
					response.getErrorCode(), contextAttributeResp);

			return new ResponseEntity<UpdateContextElementResponse>(respUpdate,
					makeHttpStatus(respUpdate.getErrorCode()));

		} else {

			UpdateContextElementResponse response = new UpdateContextElementResponse(
					new StatusCode(Code.BADREQUEST_400.getCode(),
							ReasonPhrase.BADREQUEST_400.toString(),
							"XML syntax Error!"), null);

			return new ResponseEntity<UpdateContextElementResponse>(response,
					makeHttpStatus(response.getErrorCode()));
		}

	}

	/**
	 *
	 * Executes the convenience method for appending attribute values
	 * to an individual context entity.
	 *
	 *
	 * @param EntityID
	 *            The id of the Context Entity where attribute values shall
	 *            be appended.
	 * @param request
	 *            The request body.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<AppendContextElementResponse> simpleQueryIdPost(
			@PathVariable("entityID") String EntityID,
			@RequestBody AppendContextElementRequest request) {

		logger.info(" <--- NGSI-10 has received request for Append ( POST ) to Individual context entity ---> \n");

		boolean status = validator.xmlValidation(request, sNgsi10schema);

		if (!status) {

			List<ContextElement> contextElementList = new ArrayList<ContextElement>();

			ContextElement contextElement = new ContextElement(new EntityId(
					EntityID, null, false), request.getAttributeDomainName(),
					request.getContextAttributeList(),
					request.getDomainMetadata());

			contextElementList.add(contextElement);
			UpdateContextRequest reqUpdate = new UpdateContextRequest(
					contextElementList, UpdateActionType.APPEND);

			UpdateContextResponse response = ngsiCore.updateContext(reqUpdate);

			ContextAttributeResponse contextAttributeResp = new ContextAttributeResponse(
					response.getContextElementResponse().get(0)
							.getContextElement().getContextAttributeList(),
					response.getContextElementResponse().get(0).getStatusCode());
			AppendContextElementResponse respAppend = new AppendContextElementResponse(
					response.getErrorCode(), contextAttributeResp);

			return new ResponseEntity<AppendContextElementResponse>(respAppend,
					makeHttpStatus(respAppend.getErrorCode()));

		} else {

			AppendContextElementResponse response = new AppendContextElementResponse(
					new StatusCode(Code.BADREQUEST_400.getCode(),
							ReasonPhrase.BADREQUEST_400.toString(),
							"XML syntax Error!"), null);

			return new ResponseEntity<AppendContextElementResponse>(response,
					makeHttpStatus(response.getErrorCode()));
		}

	}

	/**
	 *
	 * Executes the convenience method for removing all information
	 * about a context entity.
	 *
	 *
	 * @param EntityID
	 *            The id of the target context entity of the operation.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}", method = RequestMethod.DELETE, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<StatusCode> simpleQueryIdDelete(
			@PathVariable("entityID") String EntityID) {

		logger.info(" <--- NGSI-10 has received request for Delete ( DELETE ) to Individual context entity ---> \n");

		List<ContextElement> contextElementList = new ArrayList<ContextElement>();

		ContextElement contextElement = new ContextElement(new EntityId(
				EntityID, null, false), null, null, null);

		contextElementList.add(contextElement);
		UpdateContextRequest reqUpdate = new UpdateContextRequest(
				contextElementList, UpdateActionType.DELETE);

		UpdateContextResponse response = ngsiCore.updateContext(reqUpdate);

		StatusCode statusCode = null;

		if (response.getErrorCode() == null) {

			statusCode = new StatusCode(Code.OK_200.getCode(),
					ReasonPhrase.OK_200.toString(), null);

		} else {

			statusCode = new StatusCode(response.getErrorCode().getCode(),
					response.getErrorCode().getReasonPhrase(), response
							.getErrorCode().getDetails().toString());

		}

		return new ResponseEntity<StatusCode>(statusCode,
				makeHttpStatus(statusCode));

	}

	/**
	 * Executes the convenience method for querying an individual
	 * context entity.
	 *
	 * @param id
	 *          The id of the target context entity.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributes", method = RequestMethod.GET, consumes = { "*/*" }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<ContextElementResponse> simpleQueryAttributesContainerGet(
			@PathVariable("entityID") String id) {

		logger.info(" <--- NGSI-10 has received request for Query ( GET ) to Attribute container of individual context entity ---> \n");

		return simpleQueryIdGet(id);

	}

	/**
	 * Executes the convenience method for updating an individual
	 * context entity.
	 *
	 * @param EntityID
	 *            The id of the Context Entity to update.
	 * @param request
	 *            The request body.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributes", method = RequestMethod.PUT, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<UpdateContextElementResponse> simpleQueryAttributesContainerPut(
			@PathVariable("entityID") String EntityID,
			@RequestBody UpdateContextElementRequest request) {

		logger.info(" <--- NGSI-10 has received request for Update ( PUT ) to Attribute container of individual context entity ---> \n");

		return simpleQueryIdPut(EntityID, request);

	}



	/**
	 *
	 * Executes the convenience method for appending attribute values
	 * to an individual context entity.
	 *
	 *
	 * @param EntityID
	 *            The id of the Context Entity where attribute values shall
	 *            be appended.
	 * @param request
	 *            The request body.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributes", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<AppendContextElementResponse> simpleQueryAttributesContainerPost(
			@PathVariable("entityID") String EntityID,
			@RequestBody AppendContextElementRequest request) {

		logger.info(" <--- NGSI-10 has received request for Append ( POST ) to Attribute container of individual context entity ---> \n");

		return simpleQueryIdPost(EntityID, request);

	}

	/**
	 * Executes the convenience method for removing all information
	 * about a context entity.
	 *
	 * @param EntityID
	 *            The id of the target context entity of the operation.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributes", method = RequestMethod.DELETE, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<StatusCode> simpleQueryAttributesContainerDelete(
			@PathVariable("entityID") String EntityID) {

		logger.info(" <--- NGSI-10 has received request for Delete ( DELETE ) to Attribute container of individual context entity ---> \n");

		return simpleQueryIdDelete(EntityID);

	}

	/**
	 * Executes the convenience method for querying an individual
	 * attribute of a context entity.
	 *
	 * @param id
	 *            The id of the target context entity.
	 * @param attr
	 *            The name of the target attribute.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributes/{attributeName}", method = RequestMethod.GET, consumes = { "*/*" }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<ContextAttributeResponse> simpleQueryAttributeGet(
			@PathVariable("entityID") String id,
			@PathVariable("attributeName") String attr) {

		logger.info(" <--- NGSI-10 has received request for Query ( GET ) to Attribute of individual context entity ---> \n");

		QueryContextRequest regReq = Converter.toQueryContextRequest(id, attr);

		QueryContextResponse normalResp = ngsiCore.queryContext(regReq);

		ContextAttributeResponse response = Converter
				.toContextAttributeResponse(normalResp);

		return new ResponseEntity<ContextAttributeResponse>(response,
				makeHttpStatus(response.getStatusCode()));

	}

	/**
	 * RESOURCE = Attribute of individual context entity
	 *
	 * Base URI =
	 * http://{serverRoot}/NGSI10/contextEntities/{EntityID}/attributes
	 * /{attributeName}
	 *
	 * HTTP verbs = POST
	 *
	 * DESCRIPTION = Append context attribute value.
	 *
	 * @param EntityID
	 *            the entity id
	 * @param attributeName
	 *            the attribute name
	 * @param UpdateContextAttributeRequest
	 *            the request
	 * @return StatusCode
	 */

	/**
	 * Executes the convenience method for appending a value to
	 * an individual
	 * attribute of a context entity.
	 *
	 * @param EntityID
	 *            The id of the target context entity.
	 * @param attributeName
	 *            The name of the target attribute.
	 * @param request
	 * 				The request body.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributes/{attributeName}", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<StatusCode> simpleQueryAttributePost(
			@PathVariable("entityID") String EntityID,
			@PathVariable("attributeName") String attributeName,
			@RequestBody UpdateContextAttributeRequest request) {

		logger.info(" <--- NGSI-10 has received request for Append ( POST ) to Attribute of individual context entity ---> \n");

		boolean status = validator.xmlValidation(request, sNgsi10schema);

		if (!status) {

			List<ContextElement> contextElementList = new ArrayList<ContextElement>();
			List<ContextAttribute> contextAttributeList = new ArrayList<ContextAttribute>();
			contextAttributeList.add(new ContextAttribute(attributeName,
					request.getType(), request.getContextValue().toString(),
					request.getContextMetadata()));
			ContextElement contextElement = new ContextElement(new EntityId(
					EntityID, null, false), null, contextAttributeList, null);

			contextElementList.add(contextElement);
			UpdateContextRequest reqUpdate = new UpdateContextRequest(
					contextElementList, UpdateActionType.APPEND);

			UpdateContextResponse response = ngsiCore.updateContext(reqUpdate);

			StatusCode statusCode = new StatusCode(response.getErrorCode()
					.getCode(), response.getErrorCode().getReasonPhrase(),
					response.getErrorCode().getDetails().toString());

			return new ResponseEntity<StatusCode>(statusCode,
					makeHttpStatus(statusCode));

		} else {

			StatusCode response = new StatusCode(Code.BADREQUEST_400.getCode(),
					ReasonPhrase.BADREQUEST_400.toString(), "XML syntax Error!");

			return new ResponseEntity<StatusCode>(response,
					makeHttpStatus(response));
		}

	}

	/**
	 * RESOURCE = Attribute of individual context entity
	 *
	 * Base URI =
	 * http://{serverRoot}/NGSI10/contextEntities/{EntityID}/attributes
	 * /{attributeName}
	 *
	 * HTTP verbs = DELETE
	 *
	 * DESCRIPTION = Delete all attribute values.
	 *
	 * @param EntityID
	 *            the entity id
	 * @param attributeName
	 *            the attribute name
	 * @return StatusCode
	 */

	/**
	 * Executes the convenience method for deleting all values of
	 * an individual
	 * attribute of a context entity.
	 *
	 * @param EntityID
	 *            The id of the target context entity.
	 * @param attributeName
	 *            The name of the target attribute.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributes/{attributeName}", method = RequestMethod.DELETE, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<StatusCode> simpleQueryAttributeDelete(
			@PathVariable("entityID") String EntityID,
			@PathVariable("attributeName") String attributeName) {

		logger.info(" <--- NGSI-10 has received request for Delete ( DELETE ) to Attribute of individual context entity ---> \n");

		List<ContextElement> contextElementList = new ArrayList<ContextElement>();
		List<ContextAttribute> contextAttributeList = new ArrayList<ContextAttribute>();
		contextAttributeList.add(new ContextAttribute(attributeName, null,
				null, null));
		ContextElement contextElement = new ContextElement(new EntityId(
				EntityID, null, false), null, contextAttributeList, null);

		contextElementList.add(contextElement);
		UpdateContextRequest reqUpdate = new UpdateContextRequest(
				contextElementList, UpdateActionType.DELETE);

		UpdateContextResponse response = ngsiCore.updateContext(reqUpdate);

		StatusCode statusCode = new StatusCode(response.getErrorCode()
				.getCode(), response.getErrorCode().getReasonPhrase(), response
				.getErrorCode().getDetails().toString());

		return new ResponseEntity<StatusCode>(statusCode,
				makeHttpStatus(statusCode));
	}


	/**
	 * Executes the convenience method for retrieving a specific
	 * value instance of an attribute of a context entity.
	 *
	 * @param id
	 *            The id of the target context entity.
	 * @param attr
	 *            The name of the target attribute.
	 * @param valueID
	 * 				The target value id.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributes/{attributeName}/{valueID}", method = RequestMethod.GET, consumes = { "*/*" }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<ContextAttributeResponse> simpleQuerySpecialAttributeValueGet(
			@PathVariable("entityID") String id,
			@PathVariable("attributeName") String attr,
			@PathVariable("valueID") String valueID) {

		logger.info(" <--- NGSI-10 has received request for Query ( GET ) to Specific attribute value of individual context entity ---> \n");

		QueryContextRequest regReq = Converter.toQueryContextRequest(id, attr,
				valueID);

		QueryContextResponse normalResp = ngsiCore.queryContext(regReq);

		ContextAttributeResponse response = Converter
				.toContextAttributeResponse(normalResp);

		return new ResponseEntity<ContextAttributeResponse>(response,
				makeHttpStatus(response.getStatusCode()));

	}


	/**
	 * Executes the convenience method for updating a specific
	 * value instance of an attribute of a context entity.
	 *
	 * @param EntityID
	 *            The id of the target context entity.
	 * @param attributeName
	 *            The name of the target attribute.
	 * @param valueID
	 * 				The target value id.
	 * @param request The request body.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributes/{attributeName}/{valueID}", method = RequestMethod.PUT, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<StatusCode> simpleQuerySpecialAttributeValuePut(
			@PathVariable("entityID") String EntityID,
			@PathVariable("attributeName") String attributeName,
			@PathVariable("valueID") String valueID,
			@RequestBody UpdateContextAttributeRequest request) {

		logger.info(" <--- NGSI-10 has received request for Update ( PUT ) to Specific attribute value of individual context entity ---> \n");

		ResponseEntity<StatusCode> response = simpleQueryAttributePost(
				EntityID, attributeName, request);

		return response;

	}


	/**
	 * Executes the convenience method for deleting a specific
	 * value instance of an attribute of a context entity.
	 *
	 * @param EntityID
	 *            The id of the target context entity.
	 * @param attributeName
	 *            The name of the target attribute.
	 * @param valueID
	 * 				The target value id.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributes/{attributeName}/{valueID}", method = RequestMethod.DELETE, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<StatusCode> simpleQuerySpecialAttributeValueDelete(
			@PathVariable("entityID") String EntityID,
			@PathVariable("attributeName") String attributeName,
			@PathVariable("valueID") String valueID) {

		logger.info(" <--- NGSI-10 has received request for Delete ( DELETE ) to Specific attribute value of individual context entity ---> \n");

		ResponseEntity<StatusCode> response = simpleQueryAttributeDelete(
				EntityID, attributeName);

		return response;

	}

	/**
	 * Executes the convenience method for querying a specific attribute
	 * domain of a context entity.
	 *
	 * @param id
	 *            The id of the target context entity.
	 * @param attrDomainName
	 *            The name of the target attribute.
	 * @param scopeType
	 * 				The type of the query scope, if present.
	 * @param scopeValue
	 * 				The value of the query scope, if present.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntities/{entityID}/attributeDomains/{attributeDomainName}", method = RequestMethod.GET, consumes = { "*/*" }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public @ResponseBody
	ResponseEntity<ContextAttributeResponse> simpleQueryAttributeDomainGet(
			@PathVariable("entityID") String id,
			@PathVariable("attributeDomainName") String attrDomainName,
			@RequestParam(required = false) String scopeType,
			@RequestParam(required = false) Object scopeValue) {

		logger.info(" <--- NGSI-10 has received request for Query ( GET ) to  Attribute domain of individual context entity ---> \n");

		QueryContextRequest regReq = Converter.toQueryContextRequest(id,
				attrDomainName);

		QueryContextResponse normalResp = ngsiCore.queryContext(regReq);

		ContextAttributeResponse response = Converter
				.toContextAttributeResponse(normalResp);

		return new ResponseEntity<ContextAttributeResponse>(response,
				makeHttpStatus(response.getStatusCode()));

	}

	/**
	 * Executes the convenience method for querying all context
	 * entities having a specified type.
	 *
	 * @param typeName
	 *            The target entity type of the query.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntityTypes/{typeName}", method = RequestMethod.GET, consumes = { "*/*" }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<QueryContextResponse> simpleQueryEntityType(
			@PathVariable("typeName") URI typeName) {

		logger.info(" <--- NGSI-10 has received request for Query ( GET ) to  Context entity type ---> \n");

		List<EntityId> entityIdList = new ArrayList<EntityId>();
		List<String> attributeList = new ArrayList<String>();
		EntityId entity = new EntityId("*", typeName, true);
		entityIdList.add(entity);

		QueryContextRequest request = new QueryContextRequest(entityIdList,
				attributeList, null);

		QueryContextResponse response = ngsiCore.queryContext(request);

		return new ResponseEntity<QueryContextResponse>(response,
				makeHttpStatus(response.getErrorCode()));
	}


	/**
	 * Executes the convenience method for querying all context
	 * entities having a specified type.
	 *
	 * @param typeName
	 *            The target entity type of the query.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntityTypes/{typeName}/attributes", method = RequestMethod.GET, consumes = { "*/*" }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<QueryContextResponse> simpleQueryEntityTypeAttributeCont(
			@PathVariable("typeName") URI typeName) {

		logger.info(" <--- NGSI-10 has received request for Query ( GET ) to Attribute container of entity type ---> \n");

		return simpleQueryEntityType(typeName);
	}

	/**
	 * Executes the convenience method for querying a specific attribute from
	 * all context entities having a specified type.
	 *
	 * @param typeName
	 *            The target entity type of the query.
	 * @param attributeName
	 * 			  The target attribute of the query.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntityTypes/{typeName}/attributes/{attributeName}", method = RequestMethod.GET, consumes = { "*/*" }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<QueryContextResponse> simpleQueryEntityTypeAttribute(
			@PathVariable("typeName") URI typeName,
			@PathVariable("attributeName") String attributeName) {

		logger.info(" <--- NGSI-10 has received request for Query ( GET ) to Attribute of entity type ---> \n");

		List<EntityId> entityIdList = new ArrayList<EntityId>();
		List<String> attributeList = new ArrayList<String>();
		attributeList.add(attributeName);

		EntityId entity = new EntityId(".*", typeName, false);
		entityIdList.add(entity);

		QueryContextRequest request = new QueryContextRequest(entityIdList,
				attributeList, null);

		QueryContextResponse response = ngsiCore.queryContext(request);

		if (response.getErrorCode() == null
				&& response.getListContextElementResponse().size() != 0) {

			return new ResponseEntity<QueryContextResponse>(response,
					HttpStatus.OK);

		} else if (response.getErrorCode() != null
				&& response.getErrorCode().getCode() == 500) {

			return new ResponseEntity<QueryContextResponse>(response,
					HttpStatus.GATEWAY_TIMEOUT);
		}

		QueryContextResponse n_response = new QueryContextResponse(null,
				new StatusCode(Code.CONTEXTELEMENTNOTFOUND_404.getCode(),
						ReasonPhrase.CONTEXTELEMENTNOTFOUND_404.toString(),
						null));

		return new ResponseEntity<QueryContextResponse>(n_response,
				HttpStatus.NOT_FOUND);
	}

	/**
	 * RESOURCE = Attribute domain of entity type
	 *
	 * Base URI = http://{serverRoot}/NGSI10/contextEntityTypes/{typeName}/
	 * attributeDomains/{attributeDomainName}
	 *
	 * HTTP verbs = GET
	 *
	 * DESCRIPTION = For all context entities of the specific type, retrieve the
	 * values of all attributes belonging to the attribute domain.
	 *
	 * @param typeName
	 *            the type name
	 * @param attrDomain
	 *            the attribute domain
	 * @return QueryContextResponse
	 */

	/**
	 * Executes the convenience method for querying an attribute domain from
	 * all context entities having a specified type.
	 *
	 * @param typeName
	 *            The target entity type of the query.
	 * @param attrDomain
	 * 			  The target attribute domain of the query.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextEntityTypes/{typeName}/attributeDomains/{attributeDomainName}", method = RequestMethod.GET, consumes = { "*/*", }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public @ResponseBody
	ResponseEntity<QueryContextResponse> simpleQueryEntityTypeAttributeDomain(
			@PathVariable("typeName") URI typeName,
			@PathVariable("attributeDomainName") String attrDomain) {

		logger.info(" <--- NGSI-10 has received request for Query ( GET ) to Attribute domain of entity type  ---> \n");

		QueryContextRequest regReq = Converter.toQueryContextRequest_typeBased(
				typeName, attrDomain, null, null);

		QueryContextResponse response = ngsiCore.queryContext(regReq);

		return new ResponseEntity<QueryContextResponse>(response,
				makeHttpStatus(response.getErrorCode()));
	}

	/**
	 * RESOURCE = Subscriptions container
	 *
	 * Base URI = http://{serverRoot}/NGSI10/subscriptions
	 *
	 * HTTP verbs = POST
	 *
	 * DESCRIPTION = Create a new subscription.
	 *
	 * @param SubscribeContextRequest
	 *            the request
	 * @return SubscribeContextResponse
	 */

	/**
	 * Executes the convenience method for subscribing.
	 *
	 * @param request
	 * 		The request body of the subscription.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextSubscriptions/{subscriptionId}", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<SubscribeContextResponse> simpleSubscription(
			@RequestBody SubscribeContextRequest request) {

		logger.info(" <--- NGSI-10 has received request for Subscription ( POST ) to Subscriptions container  ---> \n");

		if (validator.xmlValidation(request, sNgsi10schema)) {
			SubscribeContextResponse response = new SubscribeContextResponse(
					null, new SubscribeError(null, new StatusCode(
							Code.BADREQUEST_400.getCode(),
							ReasonPhrase.BADREQUEST_400.toString(),
							"XML syntax Error!")));

			return new ResponseEntity<SubscribeContextResponse>(response,
					HttpStatus.BAD_REQUEST);
		}

		SubscribeContextResponse response = ngsiCore.subscribeContext(request);

		if (response.getSubscribeError() != null) {
			return new ResponseEntity<SubscribeContextResponse>(
					response,
					makeHttpStatus(response.getSubscribeError().getStatusCode()));
		}

		return new ResponseEntity<SubscribeContextResponse>(response,
				HttpStatus.OK);

	}

	/**
	 * Executes the convenience method for updating a
	 * subscription.
	 *
	 * @param request
	 * 		The request body of the subscription update.
	 * @param subscriptionId
	 * 		The identifier of the subscription to update.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextSubscriptions/{subscriptionId}", method = RequestMethod.PUT, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<UpdateContextSubscriptionResponse> updateSubscription(
			@RequestBody UpdateContextSubscriptionRequest request,
			@PathVariable("subscriptionId") String subscriptionId) {

		return updateContextSubscription(request);
	}

	/**
	 * Executes the convenience method for removing a
	 * subscription.
	 *
	 * @param subscriptionId
	 * 		The identifier of the subscription to remove.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/contextSubscriptions/{subscriptionId}", method = RequestMethod.DELETE, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<UnsubscribeContextResponse> unsubscribe(
			@PathVariable("subscriptionId") String subscriptionId) {

		logger.info(" <--- NGSI-10 has received request for Unsubscribe ( DELETE ) to Subscriptions container  ---> \n");

		return unsubscribeContext(new UnsubscribeContextRequest(subscriptionId));
	}

	/**
	 * RESOURCE = notification resource
	 *
	 * Base URI = http://{serverRoot}/notify
	 *
	 * HTTP verbs = POST
	 *
	 * DESCRIPTION = Receive notification in reaction to subscription.
	 *
	 * @param HttpServletRequest
	 *            the requester
	 * @param NotifyContextRequest
	 *            the request
	 * @return NotifyContextResponse
	 */

	/**
	 * Executes the convenience method for processing a notification.
	 *
	 * @param requester
	 * 		Represents the HTTP request message. 
	 * @param request
	 * 		The notification request body.
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi10/notify", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<NotifyContextResponse> notifyContext(
			HttpServletRequest requester,
			@RequestBody NotifyContextRequest request) {

		logger.info(" <--- NGSI-10 has received a context notification  ---> \n");

		boolean status = false;

		status = validator.xmlValidation(request, sNgsi10schema);

		if (!status) {

			NotifyContextResponse response = ngsiCore.notifyContext(request);

			return new ResponseEntity<NotifyContextResponse>(response,
					makeHttpStatus(response.getResponseCode()));

		} else {

			NotifyContextResponse response = new NotifyContextResponse(
					new StatusCode(Code.BADREQUEST_400.getCode(),
							ReasonPhrase.BADREQUEST_400.toString(),
							"XML syntax Error!"));

			return new ResponseEntity<NotifyContextResponse>(response,
					makeHttpStatus(response.getResponseCode()));
		}

	}

	/**
	 * Executes the convenience method for processing an NGSI9 notification.
	 *
	 * @param request
	 * 			The notification request body.
	 *
	 * @return The response body.
	 */
	@RequestMapping(value = "/ngsi9/notify", method = RequestMethod.POST, consumes = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON }, produces = {
			CONTENT_TYPE_XML, CONTENT_TYPE_JSON })
	public ResponseEntity<NotifyContextAvailabilityResponse> notifyContextAvailability(
			@RequestBody NotifyContextAvailabilityRequest request) {

		logger.info(" <--- NGSI-9 has received a context notification  ---> \n");

		if (validator.xmlValidation(request, sNgsi9schema)) {
			NotifyContextAvailabilityResponse response = new NotifyContextAvailabilityResponse(
					new StatusCode(Code.BADREQUEST_400.getCode(),
							ReasonPhrase.BADREQUEST_400.toString(),
							"XML syntax Error!"));
			return new ResponseEntity<NotifyContextAvailabilityResponse>(
					response, HttpStatus.BAD_REQUEST);
		}

		NotifyContextAvailabilityResponse response = ngsi9Core
				.notifyContextAvailability(request);

		return new ResponseEntity<NotifyContextAvailabilityResponse>(response,
				makeHttpStatus(response.getResponseCode()));

	}
}
