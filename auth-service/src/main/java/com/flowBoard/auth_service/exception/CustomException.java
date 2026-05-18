package com.flowBoard.auth_service.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException{
    private final HttpStatus status;

    public CustomException(String msg, HttpStatus status){
        super(msg);
        this.status = status;
    }

    public  HttpStatus getStatus(){
        return status;
    }
}
