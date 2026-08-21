package com.settle.expense.validation;

import com.settle.expense.SplitType;
import com.settle.expense.dto.CreateExpenseRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SplitDataValidator implements ConstraintValidator<ValidSplitData, CreateExpenseRequest> {

    @Override
    public boolean isValid(CreateExpenseRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getSplitType() == null) {
            return false;
        }

        SplitType type = request.getSplitType();

        switch (type) {
            case EQUAL:
                if (request.getParticipantUserIds() == null || request.getParticipantUserIds().isEmpty()) {
                    buildConstraintViolation(context, "participantUserIds is required for EQUAL split");
                    return false;
                }
                break;
            case PERCENTAGE:
                if (request.getPercentages() == null || request.getPercentages().isEmpty()) {
                    buildConstraintViolation(context, "percentages map is required for PERCENTAGE split");
                    return false;
                }
                break;
            case EXACT:
                if (request.getExactAmounts() == null || request.getExactAmounts().isEmpty()) {
                    buildConstraintViolation(context, "exactAmounts map is required for EXACT split");
                    return false;
                }
                break;
            case SHARES:
                if (request.getShares() == null || request.getShares().isEmpty()) {
                    buildConstraintViolation(context, "shares map is required for SHARES split");
                    return false;
                }
                break;
            case ITEMIZED:
                if (request.getItems() == null || request.getItems().isEmpty()) {
                    buildConstraintViolation(context, "items list is required for ITEMIZED split");
                    return false;
                }
                break;
        }

        return true;
    }

    private void buildConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
