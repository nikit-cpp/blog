package com.github.nikit.cpp.controllers;

import com.github.nikit.cpp.dto.BlogError;
import com.github.nikit.cpp.dto.ValidationError;
import com.github.nikit.cpp.exception.BadRequestException;
import com.github.nikit.cpp.exception.DataNotFoundException;
import com.github.nikit.cpp.exception.PasswordResetTokenNotFoundException;
import com.github.nikit.cpp.exception.UserAlreadyPresentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.rcp.RemoteAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

@RestControllerAdvice
public class BlogExceptionHandler {

    private Logger LOGGER = LoggerFactory.getLogger(BlogExceptionHandler.class);

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(BadRequestException.class)
    public BlogError badRequest(BadRequestException e, HttpServletResponse response) throws IOException {
        //response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        return new BlogError(HttpStatus.BAD_REQUEST.value(), "validation error", e.getMessage(), new Date().toString());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @org.springframework.web.bind.annotation.ExceptionHandler(UserAlreadyPresentException.class)
    public BlogError userAlreadyPresent(UserAlreadyPresentException e) throws IOException {
        return new BlogError(HttpStatus.FORBIDDEN.value(), "user already present", e.getMessage(), new Date().toString());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @org.springframework.web.bind.annotation.ExceptionHandler(DataNotFoundException.class)
    public BlogError dataNotFound(DataNotFoundException e) {
        return new BlogError(HttpStatus.NOT_FOUND.value(), "data not found", e.getMessage(), new Date().toString());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
    public BlogError invalid(MethodArgumentNotValidException e) throws IOException {
        Collection<ValidationError> errors = new ArrayList<>();
        e.getBindingResult().getAllErrors().forEach(objectError -> {
            if (objectError instanceof FieldError){
                FieldError fieldError = (FieldError) objectError;
                errors.add(new ValidationError(fieldError.getField(), fieldError.getRejectedValue(), fieldError.getDefaultMessage()));
            }
        });

        return new BlogError(HttpStatus.BAD_REQUEST.value(), "validation error", "validation error, see validationErrors[]", new Date().toString(), errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @org.springframework.web.bind.annotation.ExceptionHandler(PasswordResetTokenNotFoundException.class)
    public BlogError passwordResetTokenNotFound(PasswordResetTokenNotFoundException e) throws IOException {

        return new BlogError(HttpStatus.FORBIDDEN.value(), "password reset", e.getMessage(), new Date().toString());
    }


    // we hide exceptions such as SQLException so SQL didn't be present in response
    @org.springframework.web.bind.annotation.ExceptionHandler(Throwable.class)
    public void throwable(Throwable e, HttpServletResponse response) throws Throwable {
        if (
                e instanceof AccessDeniedException ||
                e instanceof AuthenticationException ||
                e instanceof RemoteAuthenticationException
        ) {throw e;} // Spring Security has own exception handling

        LOGGER.error("Unexpected exception", e);

        // response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internal error");
    }
}
