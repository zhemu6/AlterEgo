package org.zhemu.alterego.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.zhemu.alterego.exception.ErrorCode;

import java.io.Serializable;

/**
 * 相应包装嘞 封装统一的响应结果类
 * @author lushihao
 * @param <T>
 */
@Data
@NoArgsConstructor
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
