package io.mosip.compliance.toolkit.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.compliance.toolkit.config.LoggerConfiguration;
import io.mosip.compliance.toolkit.constants.AppConstants;
import io.mosip.compliance.toolkit.constants.ToolkitErrorCodes;
import io.mosip.compliance.toolkit.dto.testrun.TestRunDetailsDto;
import io.mosip.compliance.toolkit.dto.testrun.TestRunDetailsResponseDto;
import io.mosip.compliance.toolkit.dto.testrun.TestRunDto;
import io.mosip.compliance.toolkit.dto.testrun.TestCaseSummaryDto;
import io.mosip.compliance.toolkit.entity.TestRunDetailsEntity;
import io.mosip.compliance.toolkit.entity.TestRunEntity;
import io.mosip.compliance.toolkit.repository.CollectionsRepository;
import io.mosip.compliance.toolkit.repository.TestRunDetailsRepository;
import io.mosip.compliance.toolkit.repository.TestRunRepository;
import io.mosip.compliance.toolkit.util.ObjectMapperConfig;
import io.mosip.compliance.toolkit.util.RandomIdGenerator;
import io.mosip.kernel.core.authmanager.authadapter.model.AuthUserDetails;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class TestRunService {

	@Value("${mosip.toolkit.api.id.testrun.post}")
	private String postTestRunId;

	@Value("${mosip.toolkit.api.id.testrun.put}")
	private String putTestRunId;

	@Value("${mosip.toolkit.api.id.testrun.details.post}")
	private String postTestRunDetailsId;

	@Value("${mosip.toolkit.api.id.testrun.details.get}")
	private String getTestRunDetailsId;

	@Autowired
	TestRunRepository testRunRepository;

	@Autowired
	TestRunDetailsRepository testRunDetailsRepository;

	@Autowired
	CollectionsRepository collectionsRepository;

	@Autowired
	private ObjectMapperConfig objectMapperConfig;

	private Logger log = LoggerConfiguration.logConfig(TestRunService.class);

	private AuthUserDetails authUserDetails() {
		return (AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}

	private String getPartnerId() {
		String partnerId = authUserDetails().getUsername();
		return partnerId;
	}

	private String getUserBy() {
		String crBy = authUserDetails().getMail();
		return crBy;
	}

	public ResponseWrapper<TestRunDto> addTestRun(TestRunDto inputTestRun) {
		ResponseWrapper<TestRunDto> responseWrapper = new ResponseWrapper<TestRunDto>();
		TestRunDto testRun = null;
		try {
			if (Objects.nonNull(inputTestRun)) {
				ToolkitErrorCodes toolkitError = validatePartnerId(inputTestRun.getCollectionId(), getPartnerId());
				if (ToolkitErrorCodes.SUCCESS.equals(toolkitError)) {

					ObjectMapper mapper = objectMapperConfig.objectMapper();
					TestRunEntity entity = mapper.convertValue(inputTestRun, TestRunEntity.class);
					String collectionId = inputTestRun.getCollectionId();
					entity.setId(RandomIdGenerator.generateUUID(
							collectionId.substring(0, Math.min(5, collectionId.length())).toLowerCase(), "", 36));
					entity.setRunDtimes(LocalDateTime.now());
					entity.setCrBy(getUserBy());
					entity.setCrDtimes(LocalDateTime.now());
					entity.setUpdBy(null);
					entity.setUpdDtimes(null);
					entity.setDeleted(false);
					entity.setDelTime(null);

					TestRunEntity outputEntity = testRunRepository.save(entity);

					testRun = mapper.convertValue(outputEntity, TestRunDto.class);
				} else {
					List<ServiceError> serviceErrorsList = new ArrayList<>();
					ServiceError serviceError = new ServiceError();
					serviceError.setErrorCode(toolkitError.getErrorCode());
					serviceError.setMessage(toolkitError.getErrorMessage());
					serviceErrorsList.add(serviceError);
					responseWrapper.setErrors(serviceErrorsList);
				}
			} else {
				List<ServiceError> serviceErrorsList = new ArrayList<>();
				ServiceError serviceError = new ServiceError();
				serviceError.setErrorCode(ToolkitErrorCodes.INVALID_REQUEST_BODY.getErrorCode());
				serviceError.setMessage(ToolkitErrorCodes.INVALID_REQUEST_BODY.getErrorMessage());
				serviceErrorsList.add(serviceError);
				responseWrapper.setErrors(serviceErrorsList);
			}
		} catch (Exception ex) {
			log.debug("sessionId", "idType", "id", ex.getStackTrace());
			log.error("sessionId", "idType", "id",
					"In addTestRun method of TestRunService Service - " + ex.getMessage());
			List<ServiceError> serviceErrorsList = new ArrayList<>();
			ServiceError serviceError = new ServiceError();
			serviceError.setErrorCode(ToolkitErrorCodes.TESTRUN_UNABLE_TO_ADD.getErrorCode());
			serviceError.setMessage(ToolkitErrorCodes.TESTRUN_UNABLE_TO_ADD.getErrorMessage() + " " + ex.getMessage());
			serviceErrorsList.add(serviceError);
			responseWrapper.setErrors(serviceErrorsList);
		}
		responseWrapper.setId(postTestRunId);
		responseWrapper.setVersion(AppConstants.VERSION);
		responseWrapper.setResponse(testRun);
		responseWrapper.setResponsetime(LocalDateTime.now());
		return responseWrapper;
	}

	public ResponseWrapper<TestRunDto> updateTestRunExecutiionTime(TestRunDto inputTestRun) {
		ResponseWrapper<TestRunDto> responseWrapper = new ResponseWrapper<TestRunDto>();
		TestRunDto testRun = null;
		try {
			if (Objects.nonNull(inputTestRun)) {
				ToolkitErrorCodes toolkitError = validatePartnerId(inputTestRun.getCollectionId(), getPartnerId());
				if (ToolkitErrorCodes.SUCCESS.equals(toolkitError)) {

					int updateRowCount = testRunRepository.updateExecutionDateById(inputTestRun.getExecutionDtimes(),
							getUserBy(), LocalDateTime.now(), inputTestRun.getId());
					if (updateRowCount > 0) {
						testRun = inputTestRun;
					} else {
						List<ServiceError> serviceErrorsList = new ArrayList<>();
						ServiceError serviceError = new ServiceError();
						serviceError.setErrorCode(ToolkitErrorCodes.TESTRUN_UNABLE_TO_UPDATE.getErrorCode());
						serviceError.setMessage(ToolkitErrorCodes.TESTRUN_UNABLE_TO_UPDATE.getErrorMessage());
						serviceErrorsList.add(serviceError);
						responseWrapper.setErrors(serviceErrorsList);
					}
				} else {
					List<ServiceError> serviceErrorsList = new ArrayList<>();
					ServiceError serviceError = new ServiceError();
					serviceError.setErrorCode(toolkitError.getErrorCode());
					serviceError.setMessage(toolkitError.getErrorMessage());
					serviceErrorsList.add(serviceError);
					responseWrapper.setErrors(serviceErrorsList);
				}
			} else {
				List<ServiceError> serviceErrorsList = new ArrayList<>();
				ServiceError serviceError = new ServiceError();
				serviceError.setErrorCode(ToolkitErrorCodes.INVALID_REQUEST_BODY.getErrorCode());
				serviceError.setMessage(ToolkitErrorCodes.INVALID_REQUEST_BODY.getErrorMessage());
				serviceErrorsList.add(serviceError);
				responseWrapper.setErrors(serviceErrorsList);
			}
		} catch (Exception ex) {
			log.debug("sessionId", "idType", "id", ex.getStackTrace());
			log.error("sessionId", "idType", "id",
					"In updateTestRun method of TestRunService Service - " + ex.getMessage());
			List<ServiceError> serviceErrorsList = new ArrayList<>();
			ServiceError serviceError = new ServiceError();
			serviceError.setErrorCode(ToolkitErrorCodes.TESTRUN_UNABLE_TO_UPDATE.getErrorCode());
			serviceError
					.setMessage(ToolkitErrorCodes.TESTRUN_UNABLE_TO_UPDATE.getErrorMessage() + " " + ex.getMessage());
			serviceErrorsList.add(serviceError);
			responseWrapper.setErrors(serviceErrorsList);
		}
		responseWrapper.setId(putTestRunId);
		responseWrapper.setVersion(AppConstants.VERSION);
		responseWrapper.setResponse(testRun);
		responseWrapper.setResponsetime(LocalDateTime.now());
		return responseWrapper;
	}

	public ResponseWrapper<TestRunDetailsDto> addTestRunDetails(TestRunDetailsDto inputTestRunDetails) {
		ResponseWrapper<TestRunDetailsDto> responseWrapper = new ResponseWrapper<>();
		TestRunDetailsDto testRunDetails = null;
		try {
			if (Objects.nonNull(inputTestRunDetails)) {
				ToolkitErrorCodes toolkitError = validatePartnerIdByRunId(inputTestRunDetails.getRunId(),
						getPartnerId());
				if (ToolkitErrorCodes.SUCCESS.equals(toolkitError)) {
					ObjectMapper mapper = objectMapperConfig.objectMapper();

					TestRunDetailsEntity entity = mapper.convertValue(inputTestRunDetails, TestRunDetailsEntity.class);
					entity.setCrBy(getUserBy());
					entity.setCrDtimes(LocalDateTime.now());
					entity.setUpdBy(null);
					entity.setUpdDtimes(null);
					entity.setDeleted(false);
					entity.setDelTime(null);

					TestRunDetailsEntity outputEntity = testRunDetailsRepository.save(entity);

					testRunDetails = mapper.convertValue(outputEntity, TestRunDetailsDto.class);
				} else {
					List<ServiceError> serviceErrorsList = new ArrayList<>();
					ServiceError serviceError = new ServiceError();
					serviceError.setErrorCode(toolkitError.getErrorCode());
					serviceError.setMessage(toolkitError.getErrorMessage());
					serviceErrorsList.add(serviceError);
					responseWrapper.setErrors(serviceErrorsList);
				}
			} else {
				List<ServiceError> serviceErrorsList = new ArrayList<>();
				ServiceError serviceError = new ServiceError();
				serviceError.setErrorCode(ToolkitErrorCodes.INVALID_REQUEST_BODY.getErrorCode());
				serviceError.setMessage(ToolkitErrorCodes.INVALID_REQUEST_BODY.getErrorMessage());
				serviceErrorsList.add(serviceError);
				responseWrapper.setErrors(serviceErrorsList);
			}
		} catch (Exception ex) {
			log.debug("sessionId", "idType", "id", ex.getStackTrace());
			log.error("sessionId", "idType", "id",
					"In addTestRunDetails method of TestRunService Service - " + ex.getMessage());
			List<ServiceError> serviceErrorsList = new ArrayList<>();
			ServiceError serviceError = new ServiceError();
			serviceError.setErrorCode(ToolkitErrorCodes.TESTRUN_DETAILS_UNABLE_TO_ADD.getErrorCode());
			serviceError.setMessage(
					ToolkitErrorCodes.TESTRUN_DETAILS_UNABLE_TO_ADD.getErrorMessage() + " " + ex.getMessage());
			serviceErrorsList.add(serviceError);
			responseWrapper.setErrors(serviceErrorsList);
		}
		responseWrapper.setId(postTestRunDetailsId);
		responseWrapper.setVersion(AppConstants.VERSION);
		responseWrapper.setResponse(testRunDetails);
		responseWrapper.setResponsetime(LocalDateTime.now());
		return responseWrapper;
	}

	public ResponseWrapper<TestRunDetailsResponseDto> getTestRunDetails(String runId) {
		ResponseWrapper<TestRunDetailsResponseDto> responseWrapper = new ResponseWrapper<>();
		TestRunDetailsResponseDto testRunDetailsResponseDto = null;
		try {
			if (Objects.nonNull(runId)) {
				TestRunEntity entity = testRunRepository.getTestRunById(runId, getPartnerId());
				if (Objects.nonNull(entity)) {
					List<TestCaseSummaryDto> testCaseSummaryList = testRunDetailsRepository.getTestRunSummary(runId);

					testRunDetailsResponseDto = new TestRunDetailsResponseDto();
					testRunDetailsResponseDto.setCollectionId(entity.getCollectionId());
					testRunDetailsResponseDto.setRunId(entity.getId());
					testRunDetailsResponseDto.setRunDtimes(entity.getRunDtimes());
					testRunDetailsResponseDto.setExecutionDtimes(entity.getExecutionDtimes());
					testRunDetailsResponseDto.setTestcases(testCaseSummaryList);

				} else {
					List<ServiceError> serviceErrorsList = new ArrayList<>();
					ServiceError serviceError = new ServiceError();
					serviceError.setErrorCode(ToolkitErrorCodes.TESTRUN_NOT_AVAILABLE.getErrorCode());
					serviceError.setMessage(ToolkitErrorCodes.TESTRUN_NOT_AVAILABLE.getErrorMessage());
					serviceErrorsList.add(serviceError);
					responseWrapper.setErrors(serviceErrorsList);
				}
			} else {
				List<ServiceError> serviceErrorsList = new ArrayList<>();
				ServiceError serviceError = new ServiceError();
				serviceError.setErrorCode(ToolkitErrorCodes.INVALID_REQUEST_PARAM.getErrorCode());
				serviceError.setMessage(ToolkitErrorCodes.INVALID_REQUEST_PARAM.getErrorMessage());
				serviceErrorsList.add(serviceError);
				responseWrapper.setErrors(serviceErrorsList);
			}
		} catch (Exception ex) {
			log.debug("sessionId", "idType", "id", ex.getStackTrace());
			log.error("sessionId", "idType", "id",
					"In getTestRunDetails method of TestRunService Service - " + ex.getMessage());
			List<ServiceError> serviceErrorsList = new ArrayList<>();
			ServiceError serviceError = new ServiceError();
			serviceError.setErrorCode(ToolkitErrorCodes.TESTRUN_DETAILS_NOT_AVAILABLE.getErrorCode());
			serviceError.setMessage(
					ToolkitErrorCodes.TESTRUN_DETAILS_NOT_AVAILABLE.getErrorMessage() + " " + ex.getMessage());
			serviceErrorsList.add(serviceError);
			responseWrapper.setErrors(serviceErrorsList);
		}
		responseWrapper.setId(getTestRunDetailsId);
		responseWrapper.setVersion(AppConstants.VERSION);
		responseWrapper.setResponse(testRunDetailsResponseDto);
		responseWrapper.setResponsetime(LocalDateTime.now());
		return responseWrapper;
	}

	private ToolkitErrorCodes validatePartnerIdByRunId(String runId, String partnerId) {
		ToolkitErrorCodes errorCode = ToolkitErrorCodes.PARTNERID_VALIDATION_ERR;
		try {
			if (Objects.nonNull(runId)) {
				String referencePartnerid = testRunRepository.getPartnerIdByRunId(runId);
				if (Objects.nonNull(referencePartnerid)) {
					if (partnerId.equals(referencePartnerid)) {
						errorCode = ToolkitErrorCodes.SUCCESS;
					}
				}
			}
		} catch (Exception ex) {
			errorCode = ToolkitErrorCodes.PARTNERID_VALIDATION_ERR;
		}
		return errorCode;
	}

	private ToolkitErrorCodes validatePartnerId(String collectionId, String partnerId) {
		ToolkitErrorCodes errorCode = ToolkitErrorCodes.PARTNERID_VALIDATION_ERR;
		try {
			if (Objects.nonNull(collectionId)) {
				String referencePartnerid = collectionsRepository.getPartnerById(collectionId);
				if (Objects.nonNull(referencePartnerid)) {
					if (partnerId.equals(referencePartnerid)) {
						errorCode = ToolkitErrorCodes.SUCCESS;
					}
				}
			}
		} catch (Exception ex) {
			errorCode = ToolkitErrorCodes.PARTNERID_VALIDATION_ERR;
		}
		return errorCode;
	}

}