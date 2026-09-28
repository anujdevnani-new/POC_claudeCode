"""Copied verbatim from src/constants/exclusion_files.py in the main repo.

Skeleton files no layer is ever allowed to (re)generate or overwrite — used by the
PreToolUse scope-guard hook (see agents/claude_code/hooks.py) to deny Write/Edit on these
paths regardless of which layer is currently active.
"""

EXCLUSION_FILES = {
    "RFC3339DateFormat.java",
    "config/CustomErrorDecoder.java",
    "config/FeignClientConfiguration.java",
    "config/InfoEndPointApi.java",
    "config/RequestResponseLoggingFilter.java",
    "config/WebConfig.java",
    "controller/HomeController.java",
    "controller/errors/ClientSideExceptionMapper.java",
    "controller/errors/ServerSideExceptionMapper.java",
    "dto/ErrorResponseDto.java",
    "dto/ValidationErrorDto.java",
    "enumeration/EnumValidator.java",
    "enumeration/ValueOfEnum.java",
    "exceptions/ProcessingException.java",
    "exceptions/ResourceNotFoundException.java",
    "exceptions/ValidationException.java",
    "mapper/EntityMapper.java",
    "validation/AbstractValidationChain.java",
    "validation/AbstractValidationService.java",
    "validation/MapKeyNotBlank.java",
    "validation/NullOrNotEmpty.java",
    "validation/OnCreate.java",
    "validation/OnPatch.java",
    "validation/OnSignUp.java",
    "validation/OnUpdate.java",
    "validation/ValidationHandler.java",
    "validation/impl/MapKeyNotBlankValidator.java",
    "validation/impl/NullOrNotEmptyValidator.java",
    "validation/impl/PaginationParameterValidator.java",
    "validation/util/CommonValidationUtil.java",
}
