package com.example.demo.mapper;

import com.example.demo.dto.UserManageVO;
import com.example.demo.pojo.Login;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LoginMapper {

    /** 根据手机号查询 */
    Login findByPhone(@Param("phone") String phone);

    /** 根据邮箱查询 */
    Login findByEmail(@Param("email") String email);

    /** 根据 userId 查询 */
    Login findByUserId(@Param("userId") Long userId);

    /** 手机号注册 */
    int insertByPhone(@Param("phone") String phone,
                      @Param("password") String password,
                      @Param("status") Integer status);

    /** 邮箱注册 */
    int insertByEmail(@Param("email") String email,
                      @Param("password") String password,
                      @Param("status") Integer status);

    /** 根据手机号更新密码 */
    int updatePasswordByPhone(@Param("phone") String phone,
                              @Param("password") String password);

    /** 根据邮箱更新密码 */
    int updatePasswordByEmail(@Param("email") String email,
                              @Param("password") String password);

    /** 更新手机号（按 userId） */
    int updatePhone(@Param("userId") Long userId,
                    @Param("phone") String phone);

    /** 更新邮箱（按 userId） */
    int updateEmail(@Param("userId") Long userId,
                    @Param("email") String email);

    /** 查询全部用户管理信息 */
    List<UserManageVO> listAllUsers();

    /** 修改用户权限（按 userId） */
    int updateStatus(@Param("userId") Long userId,
                     @Param("status") Integer status);

    /** 按 userId 删除登录信息 */
    int deleteByUserId(@Param("userId") Long userId);
}
