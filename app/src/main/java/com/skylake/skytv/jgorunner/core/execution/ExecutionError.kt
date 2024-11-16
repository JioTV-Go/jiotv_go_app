package com.skylake.skytv.jgorunner.core.execution

class ExecutionError : Exception {
    val errorType: ExecutionErrorType

    constructor(errorType: ExecutionErrorType, message: String) : super(message){
        this.errorType = errorType
    }
    constructor(errorType: ExecutionErrorType, message: String, cause: Throwable) : super(message, cause) {
        this.errorType = errorType
    }

    enum class ExecutionErrorType {
        BINARY_NOT_FOUND,
        BINARY_UNKNOWN_ERROR
    }
}