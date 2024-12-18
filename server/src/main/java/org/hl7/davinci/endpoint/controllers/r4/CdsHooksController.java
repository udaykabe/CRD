package org.hl7.davinci.endpoint.controllers.r4;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.cdshooks.CdsResponse;
import org.hl7.davinci.endpoint.Utils;
import org.hl7.davinci.endpoint.cdshooks.services.crd.CdsServiceInformation;
import org.hl7.davinci.endpoint.cdshooks.services.crd.r4.*;
import org.hl7.davinci.r4.crdhook.CrdPrefetch;
import org.hl7.davinci.r4.crdhook.appointmentbook.AppointmentBookRequest;
import org.hl7.davinci.r4.crdhook.encounterdischarge.EncounterDischargeRequest;
import org.hl7.davinci.r4.crdhook.encounterstart.EncounterStartRequest;
import org.hl7.davinci.r4.crdhook.orderselect.OrderSelectRequest;
import org.hl7.davinci.r4.crdhook.ordersign.OrderSignRequest;
import org.hl7.davinci.r4.crdhook.orderdispatch.OrderDispatchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("r4_CdsHooksController")
public class CdsHooksController {

  static final Logger logger = LoggerFactory.getLogger(CdsHooksController.class);

  static final String FHIR_RELEASE = "/r4";
  static final String URL_BASE = "/cds-services";


  @Autowired private OrderSelectService orderSelectService;
  @Autowired private OrderSignService orderSignService;
  @Autowired private AppointmentBookService appointmentBookService;
  @Autowired private OrderDispatchService orderDispatchService;
  @Autowired private EncounterStartService encounterStartService;
  @Autowired private EncounterDischargeService encounterDischargeService;

  /**
   * The FHIR r4 services discovery endpoint.
   * @return A services object containing an array of all services available on this server
   */
  @CrossOrigin
  @GetMapping(value = FHIR_RELEASE + URL_BASE)
  public CdsServiceInformation serviceDiscovery() {
    logger.info("r4/serviceDiscovery");
    CdsServiceInformation serviceInformation = new CdsServiceInformation();
    serviceInformation.addServicesItem(orderSignService);
    serviceInformation.addServicesItem(orderSelectService);
    serviceInformation.addServicesItem(appointmentBookService);
    serviceInformation.addServicesItem(orderDispatchService);
    serviceInformation.addServicesItem(encounterStartService);
    serviceInformation.addServicesItem(encounterDischargeService);
    return serviceInformation;
  }

  /**
   * The coverage requirement discovery endpoint for the order select hook.
   * @param request An order select triggered cds request
   * @return The card response
   */
  @CrossOrigin
  @PostMapping(value = FHIR_RELEASE + URL_BASE + "/" + OrderSelectService.ID,
      consumes = "application/json;charset=UTF-8")
  public CdsResponse handleOrderSelect(@Valid @RequestBody OrderSelectRequest request, final HttpServletRequest httpServletRequest) {
    logger.info("r4/handleOrderSelect");
    if (request.getPrefetch() == null) {
      request.setPrefetch(new CrdPrefetch());
    }
    return orderSelectService.handleRequest(request, Utils.getApplicationBaseUrl(httpServletRequest));
  }

  /**
   * The coverage requirement discovery endpoint for the order sign hook.
   * @param request An order sign triggered cds request
   * @return The card response
   */
  @CrossOrigin
  @PostMapping(value = FHIR_RELEASE + URL_BASE + "/" + OrderSignService.ID,
      consumes = "application/json;charset=UTF-8")
  public CdsResponse handleOrderSign(@Valid @RequestBody OrderSignRequest request, final HttpServletRequest httpServletRequest) {
    logger.info("r4/handleOrderSign");
    if (request.getPrefetch() == null) {
      request.setPrefetch(new CrdPrefetch());
    }
    return orderSignService.handleRequest(request, Utils.getApplicationBaseUrl(httpServletRequest));
  }
  
  
  /**
   * The coverage requirement discovery endpoint for the appointment-book hook.
   * @param request An appointment-book triggered cds request
   * @return The card response
   */
  @CrossOrigin
  @PostMapping(value = FHIR_RELEASE + URL_BASE + "/" + AppointmentBookService.ID,
      consumes = "application/json;charset=UTF-8")
  public CdsResponse handleAppointmentBook(@Valid @RequestBody AppointmentBookRequest request, final HttpServletRequest httpServletRequest) {
    logger.info("r4/handleAppointmentBook");
    if (request.getPrefetch() == null) {
      request.setPrefetch(new CrdPrefetch());
    }
    return appointmentBookService.handleRequest(request, Utils.getApplicationBaseUrl(httpServletRequest));
  }

  @CrossOrigin
  @PostMapping(value = FHIR_RELEASE + URL_BASE + "/" + EncounterStartService.ID,
    consumes = "application/json;charset=UTF-8")
  public CdsResponse handleEncounterStart(@Valid @RequestBody EncounterStartRequest request, final HttpServletRequest httpServletRequest) {
    logger.info("r4/handleEncounterStart");
    if (request.getPrefetch() == null) {
      request.setPrefetch(new CrdPrefetch());
    }
    return encounterStartService.handleRequest(request, Utils.getApplicationBaseUrl(httpServletRequest));
  }

  @CrossOrigin
  @PostMapping(value = FHIR_RELEASE + URL_BASE + "/" + EncounterDischargeService.ID,
          consumes = "application/json;charset=UTF-8")
  public CdsResponse handleEncounterStart(@Valid @RequestBody EncounterDischargeRequest request, final HttpServletRequest httpServletRequest) {
    logger.info("r4/handleEncounterDischarge");
    if (request.getPrefetch() == null) {
      request.setPrefetch(new CrdPrefetch());
    }
    return encounterDischargeService.handleRequest(request, Utils.getApplicationBaseUrl(httpServletRequest));
  }

  
  /**
   * The coverage requirement discovery endpoint for the appointment-book hook.
   * @return The card response
   */
  @CrossOrigin
  @PostMapping(value = FHIR_RELEASE + URL_BASE + "/" + "test-hook",
      consumes = "application/json;charset=UTF-8")
  public String handleTestHook(final HttpServletRequest httpServletRequest) {
    logger.info("r4/handleTestHook");
    
    return "[]";
  }

  /**
   * The coverage requirement discovery endpoint for the order dispatch hook.
   * @param request An order select triggered cds request
   * @return The card response
   */
  @CrossOrigin
  @PostMapping(value = FHIR_RELEASE + URL_BASE + "/" + OrderDispatchService.ID,
          consumes = "application/json;charset=UTF-8")
  public CdsResponse handleOrderDispatch(@Valid @RequestBody OrderDispatchRequest request, final HttpServletRequest httpServletRequest) {
    logger.info("r4/handleOrderDispatch");
    if (request.getPrefetch() == null) {
      request.setPrefetch(new CrdPrefetch());
    }
    return orderDispatchService.handleRequest(request, Utils.getApplicationBaseUrl(httpServletRequest));
  }
}


