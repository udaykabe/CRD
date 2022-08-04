package org.hl7.davinci.endpoint.cdshooks.services.crd.r4;

import org.hl7.davinci.RequestIncompleteException;
import org.hl7.davinci.endpoint.cql.CqlRule;
import org.hl7.davinci.endpoint.cql.r4.CqlExecutionContextBuilder;
import org.hl7.davinci.endpoint.database.RuleMapping;
import org.hl7.davinci.endpoint.files.FileStore;
import org.hl7.davinci.endpoint.rules.CoverageRequirementRuleCriteria;
import org.hl7.davinci.endpoint.rules.CoverageRequirementRuleResult;
import org.hl7.davinci.r4.Utilities;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class FhirBundleProcessor {
  static final Logger logger = LoggerFactory.getLogger(FhirBundleProcessor.class);

  private FileStore fileStore;

  private String baseUrl;
  private List<String> selections;
  private List<CoverageRequirementRuleResult> results = new ArrayList<>();


  public FhirBundleProcessor(FileStore fileStore, String baseUrl, List<String> selections) {
    this.fileStore = fileStore;
    this.baseUrl = baseUrl;
    this.selections = selections;
  }

  public FhirBundleProcessor(FileStore fileStore, String baseUrl) {
    this(fileStore, baseUrl, new ArrayList<>());
  }

  public List<CoverageRequirementRuleResult> getResults() { return results; }

  public void processDeviceRequests(Bundle deviceRequestBundle, Bundle coverageBundle) {
    List<DeviceRequest> deviceRequestList = Utilities.getResourcesOfTypeFromBundle(DeviceRequest.class, deviceRequestBundle);
    List<Patient> patients = Utilities.getResourcesOfTypeFromBundle(Patient.class, deviceRequestBundle);
    logger.info("r4/FhirBundleProcessor::processDeviceRequests: Found " + patients.size() + " patients.");
    List<Organization> payorList = Utilities.getResourcesOfTypeFromBundle(Organization.class, coverageBundle); // TODO - do something with the coverage.
    if (deviceRequestList.isEmpty()) return;
    
    logger.info("r4/FhirBundleProcessor::processDeviceRequests: " + deviceRequestList.size() + " DeviceRequest(s) found");

    for (DeviceRequest deviceRequest : deviceRequestList) {
      if (idInSelectionsList(deviceRequest.getId())) {
        List<CoverageRequirementRuleCriteria> criteriaList = createCriteriaList(deviceRequest.getCodeCodeableConcept(), deviceRequest.getInsurance(), payorList);
        
        String patientReference = deviceRequest.getSubject().getReference();
        List<Patient> referencedPrefetechedPatients = extractReferencedResources(patients, patientReference);
            
        if (referencedPrefetechedPatients.size() < 1) {
          logger.error("r4/FhirBundleProcessor::processDeviceRequests: ERROR - Device Request '"
              + deviceRequest.getId() + "' does not contain a reference to any prefetched patients. Resource contains patient reference '"
              + patientReference + "' and prefetch contains patients " + patients.stream().map(patient -> patient.getId()).collect(Collectors.toSet()) + ".");
        }

        Patient patientToUse = referencedPrefetechedPatients.get(0);
        buildExecutionContexts(criteriaList, patientToUse, "device_request", deviceRequest);
      }
    }
  }

  public void processMedicationRequests(Bundle medicationRequestBundle, Bundle coverageBundle) {
    List<MedicationRequest> medicationRequestList = Utilities.getResourcesOfTypeFromBundle(MedicationRequest.class, medicationRequestBundle);
    List<Patient> patients = Utilities.getResourcesOfTypeFromBundle(Patient.class, medicationRequestBundle);
    List<Organization> payorList = Utilities.getResourcesOfTypeFromBundle(Organization.class, coverageBundle);
    if (medicationRequestList.isEmpty()) return;

    logger.info("r4/FhirBundleProcessor::processMedicationRequests: MedicationRequest(s) found");

    for (MedicationRequest medicationRequest : medicationRequestList) {
      if (idInSelectionsList(medicationRequest.getId())) {
        String patientReference = medicationRequest.getSubject().getReference();

        List<Patient> referencedPrefetechedPatients = extractReferencedResources(patients, patientReference);
        if (referencedPrefetechedPatients.size() < 1) {
          logger.error("r4/FhirBundleProcessor::processMedicationRequests: ERROR - Medication Request '"
              + medicationRequest.getId() + "' does not contain a reference to any prefetched patients. Resource contains patient reference '"
              + patientReference + "' and prefetch contains patients " + patients.stream().map(patient -> patient.getId()).collect(Collectors.toSet()) + ".");
        }
        
        List<CoverageRequirementRuleCriteria> criteriaList = createCriteriaList(medicationRequest.getMedicationCodeableConcept(), medicationRequest.getInsurance(), payorList);
        Patient patientToUse = referencedPrefetechedPatients.get(0);
        buildExecutionContexts(criteriaList, patientToUse, "medication_request", medicationRequest);
      }
    }
  }

  public void processMedicationDispenses(Bundle medicationDispenseBundle, Bundle coverageBundle) {
    List<MedicationDispense> medicationDispenseList = Utilities.getResourcesOfTypeFromBundle(MedicationDispense.class, medicationDispenseBundle);
    List<Patient> patients = Utilities.getResourcesOfTypeFromBundle(Patient.class, medicationDispenseBundle);
    List<Organization> payorList = Utilities.getResourcesOfTypeFromBundle(Organization.class, coverageBundle);
    List<Organization> medicationPayorList = Utilities.getResourcesOfTypeFromBundle(Organization.class,
        medicationDispenseBundle);
    payorList.addAll(medicationPayorList);
    if (medicationDispenseList.isEmpty()) return;

    logger.info("r4/FhirBundleProcessor::processMedicationDispenses: MedicationDispense(s) found");

    for (MedicationDispense medicationDispense : medicationDispenseList) {
      if (idInSelectionsList(medicationDispense.getId())) {
        String patientReference = medicationDispense.getSubject().getReference();
        List<Patient> referencedPrefetechedPatients = extractReferencedResources(patients, patientReference);
        if (referencedPrefetechedPatients.size() < 1) {
          logger.error("r4/FhirBundleProcessor::processMedicationDispenses: ERROR - Medication Dispense '"
              + medicationDispense.getId() + "' does not contain a reference to any prefetched patients. Resource contains patient reference '"
              + patientReference + "' and prefetch contains patients " + patients.stream().map(patient -> patient.getId()).collect(Collectors.toSet()) + ".");
          return;
        }
        List<CoverageRequirementRuleCriteria> criteriaList = createCriteriaList(medicationDispense.getMedicationCodeableConcept(), null, payorList);
        Patient patientToUse = referencedPrefetechedPatients.get(0);
        buildExecutionContexts(criteriaList,patientToUse, "medication_dispense", medicationDispense);
      }
    }
  }

  public void processServiceRequests(Bundle serviceRequestBundle, Bundle coverageBundle) {
    List<Organization> payorList = Utilities.getResourcesOfTypeFromBundle(Organization.class, coverageBundle);
    List<ServiceRequest> serviceRequestList = Utilities.getResourcesOfTypeFromBundle(ServiceRequest.class, serviceRequestBundle);
    List<Patient> patients = Utilities.getResourcesOfTypeFromBundle(Patient.class, serviceRequestBundle);
    if (serviceRequestList.isEmpty()) return;

    logger.info("r4/FhirBundleProcessor::getAndProcessServiceRequests: ServiceRequest(s) found");

    for (ServiceRequest serviceRequest : serviceRequestList) {
      if (idInSelectionsList(serviceRequest.getId())) {
        String patientReference = serviceRequest.getSubject().getReference();
        List<Patient> referencedPrefetechedPatients = extractReferencedResources(patients, patientReference);
        if (referencedPrefetechedPatients.size() < 1) {
          logger.error("r4/FhirBundleProcessor::processServiceRequests: ERROR - Service Request '"
              + serviceRequest.getId() + "' does not contain a reference to any prefetched patients. Resource contains patient reference '"
              + patientReference + "' and prefetch contains patients " + patients.stream().map(patient -> patient.getId()).collect(Collectors.toSet()) + ".");

        }
        List<CoverageRequirementRuleCriteria> criteriaList = createCriteriaList(serviceRequest.getCode(), serviceRequest.getInsurance(), payorList);
        Patient patientToUse = referencedPrefetechedPatients.iterator().next();
        logger.info("r4/FhirBundleProcessor::processMedicationDispenses: Found Patient '" + patientToUse + "'.");
        buildExecutionContexts(criteriaList, patientToUse, "service_request", serviceRequest);
      }
    }
  }

  public void processOrderSelectMedicationStatements(Bundle medicationRequestBundle, Bundle medicationStatementBundle, Bundle coverageBundle) {
    List<MedicationRequest> medicationRequestList = Utilities.getResourcesOfTypeFromBundle(MedicationRequest.class, medicationRequestBundle);
    List<MedicationStatement> medicationStatementList = Utilities.getResourcesOfTypeFromBundle(MedicationStatement.class, medicationStatementBundle);

    List<Patient> medStatementPatients = Utilities.getResourcesOfTypeFromBundle(Patient.class, medicationStatementBundle);
    List<Organization> payorList = Utilities.getResourcesOfTypeFromBundle(Organization.class, coverageBundle);

    if (medicationRequestList.isEmpty()) return;

    logger.info("r4/FhirBundleProcessor::processOrderSelectMedicationStatements: MedicationRequests(s) found");

    // process each of the MedicationRequests
    for (MedicationRequest medicationRequest : medicationRequestList) {
      if (idInSelectionsList(medicationRequest.getId())) {

        // run on each of the MedicationStatements
        for (MedicationStatement medicationStatement : medicationStatementList) {
          logger.info("r4/FhirBundleProcessor::processOrderSelectMedicationStatements: MedicationStatement found: " + medicationStatement.getId());
          String patientReference = medicationStatement.getSubject().getReference();
          List<Patient> referencedPrefetechedPatients = extractReferencedResources(medStatementPatients, patientReference);
          if (referencedPrefetechedPatients.size() < 1) {
            logger.error("r4/FhirBundleProcessor::processMedicationStatements: ERROR - Medication Statement '"
                + medicationStatement.getId() + "' does not contain a reference to any prefetched patients. Resource contains patient reference '"
                + patientReference + "' and prefetch contains patients " + medStatementPatients.stream().map(patient -> patient.getId()).collect(Collectors.toSet()) + ".");
          }

          List<CoverageRequirementRuleCriteria> criteriaList = createCriteriaList(medicationRequest.getMedicationCodeableConcept(), medicationRequest.getInsurance(), payorList);
          Patient patientToUse = referencedPrefetechedPatients.get(0);

          HashMap<String, Resource> cqlParams = new HashMap<>();
          cqlParams.put("Patient", (Patient) patientToUse);
          cqlParams.put("medication_request", medicationRequest);
          cqlParams.put("medication_statement", medicationStatement);

          buildExecutionContexts(criteriaList, cqlParams);
        }
      }
    }
  }

  private List<CoverageRequirementRuleCriteria> createCriteriaList(CodeableConcept codeableConcept, List<Reference> insurance, List<Organization> payorList) {
    try {
      List<Coding> codings = codeableConcept.getCoding();
      if (codings.size() > 0) {
        logger.info("r4/FhirBundleProcessor::createCriteriaList: code[0]: " + codings.get(0).getCode() + " - " + codings.get(0).getSystem());
      } else {
        logger.info("r4/FhirBundleProcessor::createCriteriaList: empty codes list!");
      }

      List<Organization> payors = new ArrayList<>();
      if (insurance != null) {
        List<Coverage> coverages = insurance.stream()
            .map(reference -> (Coverage) reference.getResource()).collect(Collectors.toList());
        // Remove null coverages that may not have resolved.
        coverages = coverages.stream().filter(coverage -> coverage != null).collect(Collectors.toList());
        payors.addAll(Utilities.getPayors(coverages));
      }
      if (payorList != null) {
        payors.addAll(payorList);
      }

      if (payors.size() > 0) {
        logger.info("r4/FhirBundleProcessor::createCriteriaList: payer[0]: " + payors.get(0).getName());
      } else {
        // default to CMS if no payer was provided
        logger.info("r4/FhirBundleProcessor::createCriteriaList: empty payers list, working around by adding CMS!");
        Organization org = new Organization().setName("Centers for Medicare and Medicaid Services");
        org.setId("75f39025-65db-43c8-9127-693cdf75e712"); // how to get ID
        payors.add(org);
        // remove the exception to use CMS if no payer is provided
        // JIRA ticket https://jira.mitre.org/browse/DMEERX-894
        // throw new RequestIncompleteException("No Payer found in coverage resource, cannot find documentation.");
      }

      List<CoverageRequirementRuleCriteria> criteriaList = CoverageRequirementRuleCriteria
          .createQueriesFromR4(codings, payors);
      return criteriaList;
    } catch (RequestIncompleteException e) {
      // rethrow incomplete request exceptions
      throw e;
    } catch (Exception e) {
      // catch all remaining exceptions
      System.out.println(e);
      throw new RequestIncompleteException("Unable to parse list of codes, codesystems, and payors from a device request.");
    }
  }

  private void buildExecutionContexts(List<CoverageRequirementRuleCriteria> criteriaList, Patient patient, String requestType, DomainResource request) {
    System.out.println("buildExecutionContexts::PATIENT: " + patient);
    HashMap<String, Resource> cqlParams = new HashMap<>();
    cqlParams.put("Patient", patient);
    cqlParams.put(requestType, request);
    buildExecutionContexts(criteriaList, cqlParams);
  }

  private void buildExecutionContexts(List<CoverageRequirementRuleCriteria> criteriaList, HashMap<String, Resource> cqlParams) {

    for (CoverageRequirementRuleCriteria criteria : criteriaList) {
      logger.info("FhirBundleProcessor::buildExecutionContexts() criteria: " + criteria.toString());
      List<RuleMapping> rules = fileStore.findRules(criteria);

      for (RuleMapping rule: rules) {
        CoverageRequirementRuleResult result = new CoverageRequirementRuleResult();
        result.setCriteria(criteria).setTopic(rule.getTopic());
        try {
          logger.info("FhirBundleProcessor::buildExecutionContexts() found rule topic: " + rule.getTopic());

          //get the CqlRule
          CqlRule cqlRule = fileStore.getCqlRule(rule.getTopic(), rule.getFhirVersion());
          result.setContext(CqlExecutionContextBuilder.getExecutionContext(cqlRule, cqlParams, baseUrl));
          results.add(result);
        } catch (Exception e) {
          logger.info("r4/FhirBundleProcessor::buildExecutionContexts: failed processing cql bundle: " + e.getMessage());
        }
      }
    }
  }

  private String stripResourceType(String identifier) {
    int indexOfDivider = identifier.indexOf('/');
    if (indexOfDivider+1 == identifier.length()) {
      // remove the trailing '/'
      return identifier.substring(0, indexOfDivider);
    } else {
      return identifier.substring(indexOfDivider+1);
    }
  }

  private boolean idInSelectionsList(String identifier) {
    if (this.selections.isEmpty()) {
      // if selections list is empty, just assume we should process the request
      return true;
    } else {
      for ( String selection : selections) {
        if (identifier.contains(stripResourceType(selection))) {
          logger.info("r4/FhirBundleProcessor::idInSelectionsList(" + identifier + "): identifier found in selections list");
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Extracts the resources from the list that have the given id.
   * @param <R> The resource type to extract from.
   * @param resources The resources to extact from.
   * @param resourceId  The resource Id to extract with.
   * @return  The list of resources with the given Id.
   */
  private static <R extends Resource> List<R> extractReferencedResources(List<R> resources, String resourceId) {
    return resources.stream().filter((currentResource) -> {
      String currentId = currentResource.getId();
      return currentId != null && (currentId.contains(resourceId) || resourceId.contains(currentId));
    }).collect(Collectors.toList());
  }

}