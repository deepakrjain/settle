package com.settle.expense.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SplitDataValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSplitData {
    String message() default "Invalid split data for chosen split type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
