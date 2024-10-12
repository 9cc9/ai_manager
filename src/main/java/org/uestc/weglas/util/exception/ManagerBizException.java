package org.uestc.weglas.util.exception;

/**
 * 定义通用业务异常
 * TODO 错误码
 *
 * @author yingxian.cyx
 * @date Created in 2024/6/21
 */
public class ManagerBizException extends RuntimeException {

    public ManagerBizException() {
        super();
    }

    public ManagerBizException(String message) {
        super(message);
    }

    public ManagerBizException(String message, Throwable cause) {
        super(message, cause);
    }

    public ManagerBizException(Throwable cause) {
        super(cause);
    }
}
