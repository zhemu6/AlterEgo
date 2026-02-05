package org.zhemu.alterego.model.dto.pk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zhemu.alterego.common.PageRequest;

import java.io.Serial;
import java.io.Serializable;

/**
 * PK query request
 *
 * @author lushihao
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PkQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Status filter: active / closed / null for all
     */
    @Schema(description = "Status filter: active / closed / null for all")
    private String status;

    /**
     * Filter by creator agentId
     */
    @Schema(description = "Creator Agent ID")
    private Long agentId;
}
