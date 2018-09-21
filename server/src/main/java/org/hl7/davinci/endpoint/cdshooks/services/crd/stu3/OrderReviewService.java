package org.hl7.davinci.endpoint.cdshooks.services.crd.stu3;

import org.cdshooks.CdsResponse;
import java.util.Arrays;
import org.cdshooks.CdsService;
import org.cdshooks.Hook;
import org.cdshooks.Prefetch;
import java.util.List;
import javax.validation.Valid;
import org.hl7.davinci.endpoint.components.CardBuilder;
import org.hl7.davinci.endpoint.components.PrefetchHydrator;
import org.hl7.davinci.endpoint.database.CoverageRequirementRule;
import org.hl7.davinci.endpoint.database.CoverageRequirementRuleFinder;
import org.hl7.davinci.stu3.FhirComponents;
import org.hl7.davinci.stu3.Utilities;
import org.hl7.davinci.stu3.crdhook.CrdPrefetch;
import org.hl7.davinci.stu3.crdhook.CrdPrefetchTemplateElements;
import org.hl7.davinci.stu3.crdhook.CrdPrefetchTemplateElements.PrefetchTemplateElement;
import org.hl7.davinci.stu3.crdhook.orderreview.OrderReviewRequest;
import org.hl7.davinci.stu3.fhirresources.DaVinciDeviceRequest;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.exceptions.FHIRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;


@Component("stu3_OrderReviewService")
public class OrderReviewService extends CdsService {

  public static final String ID = "order-review-crd";
  public static final String TITLE = "order-review Coverage Requirements Discovery";
  public static final Hook HOOK = Hook.ORDER_REVIEW;
  public static final String DESCRIPTION =
      "Get information regarding the coverage requirements for durable medical equipment";
  public static final Prefetch PREFETCH;
  static final Logger logger = LoggerFactory.getLogger(OrderReviewService.class);

  static {
    PREFETCH = new Prefetch();
    List<PrefetchTemplateElement> elements = Arrays.asList(
        CrdPrefetchTemplateElements.DEVICE_REQUEST_BUNDLE,
        CrdPrefetchTemplateElements.SUPPLY_REQUEST_BUNDLE,
        CrdPrefetchTemplateElements.NUTRITION_ORDER_BUNDLE,
        CrdPrefetchTemplateElements.MEDICATION_REQUEST_BUNDLE,
        CrdPrefetchTemplateElements.PROCEDURE_REQUEST_BUNDLE,
        CrdPrefetchTemplateElements.REFERRAL_REQUEST_BUNDLE
    );
    for (PrefetchTemplateElement element : elements) {
      PREFETCH.put(element.getKey(), element.getQuery());
    }
  }

  @Autowired
  CoverageRequirementRuleFinder ruleFinder;

  public OrderReviewService() {
    super(ID, HOOK, TITLE, DESCRIPTION, PREFETCH);
  }

  /**
   * Handle the post request to the service.
   *
   * @param request The json request, parsed.
   */
  public CdsResponse handleRequest(@Valid @RequestBody OrderReviewRequest request) {
    logger.info("handleRequest: start");
    logger.info("Order bundle size: " + request.getContext().getOrders().getEntry().size());

    //note currently we only use the device request if its in the prefetch or we get it into
    //the prefetch, so we dont use it if its just in the context since it wont have patient etc.
    FhirComponents fhirComponents = FhirComponents.getInstance();
    if (request.getPrefetch() == null)
      request.setPrefetch(new CrdPrefetch());
    PrefetchHydrator prefetchHydrator = new PrefetchHydrator<Bundle>(this, request,
        fhirComponents.getFhirContext());
    prefetchHydrator.hydrate(); //prefetch is now as hydrated as possible

    CdsResponse response = new CdsResponse();

    Bundle deviceRequestBundle = request.getPrefetch().getDeviceRequestBundle();
    if (deviceRequestBundle == null) {
      logger.error("Prefetch deviceRequestBundle not a bundle");
      response.addCard(CardBuilder.summaryCard(
          "deviceRequestBundle could not be (pre)fetched in this request "));
      return response;
    }
    List<DaVinciDeviceRequest> deviceRequestList = Utilities.getResourcesOfTypeFromBundle(
        DaVinciDeviceRequest.class, deviceRequestBundle);

    for (DaVinciDeviceRequest deviceRequest : deviceRequestList) {

      Patient patient = null;
      CodeableConcept cc = null;
      try {
        cc = deviceRequest.getCodeCodeableConcept();
      } catch (FHIRException fe) {
        response
            .addCard(CardBuilder.summaryCard("Unable to parse the device code out of the request"));
      }

      // See if the patient is in the prefetch
      try {
        patient = (Patient) deviceRequest.getSubject().getResource();
      } catch (Exception e) {
        response
            .addCard(CardBuilder.summaryCard("No patient could be (pre)fetched in this request"));
      }
      // get insurance with deviceRequest.getInsurance().get(0).getResource()

      if (patient != null && cc != null) {
        int patientAge = Utilities.calculateAge(patient);
        List<CoverageRequirementRule> coverageRequirementRules = ruleFinder
            .findRules(patientAge, patient.getGender(), cc.getCoding().get(0).getCode(),
                cc.getCoding().get(0).getSystem());
        if (coverageRequirementRules.size() == 0) {
          response.addCard(CardBuilder.summaryCard("No documentation rules found"));
        } else {
          for (CoverageRequirementRule rule: coverageRequirementRules) {
            response.addCard(CardBuilder.transform(rule));
          }
        }
      }
    }

    logger.info("handleRequest: end");
    return response;
  }

}
