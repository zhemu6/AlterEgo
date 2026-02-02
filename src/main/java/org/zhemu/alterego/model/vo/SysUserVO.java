package org.zhemu.alterego.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zhemu.alterego.model.entity.SysUser;

import java.io.Serial;
import java.io.Serializable;

/**
 * 返回给前端用户
 * @author lushihao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysUserVO implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;


    /**
     * 用户角色 user admin
     */
    private String userRole;


    /**
     * 邮箱
     */
    private String email;

    @Serial
    private static final long serialVersionUID = 1L;

    public static SysUserVO objToVo(SysUser user) {
        if (user == null) {
            return null;
        }
        SysUserVO userVO = new SysUserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }
}