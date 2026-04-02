package com.jstudy.inout.common.extra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
 

@AllArgsConstructor
@Builder
@Data
public class ErrorResponse {
	
	private final int status;
    private final String code;
    private final String message;
}
