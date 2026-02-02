package org.zhemu.alterego.common;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除请求类
 * @author lushihao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeleteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * id 根据id删除
     */
    @NotNull(message = "id不能为空")
    @Min(value = 1, message = "id必须大于0")
    private Long id;

}
