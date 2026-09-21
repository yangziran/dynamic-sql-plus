package cn.kunter.dynamic.example.entity;

import cn.kunter.dynamic.annotations.DynamicMapper;
import cn.kunter.dynamic.annotations.TableColumn;
import cn.kunter.dynamic.annotations.TableId;

/**
 * 用户实体类。
 * @author yangziran
 */
@DynamicMapper
public class UserEo {

    @TableId(autoIncrement = true)
    private Long id;
    private String username;

    @TableColumn(value = "user_status")
    private Integer status;

    @TableColumn(ignore = true)
    private String temporaryToken;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTemporaryToken() {
        return temporaryToken;
    }

    public void setTemporaryToken(String temporaryToken) {
        this.temporaryToken = temporaryToken;
    }
}
