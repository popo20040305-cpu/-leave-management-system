package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.MenuPojo;
import edu.hbmd.taotao.qingjia.vo.MenuVo;

import java.util.List;
import java.util.Set;

public interface MenuService extends IService<MenuPojo> {
    //根据菜单ids查询菜单信息
    List<MenuPojo> getMenusByIds(Set<Integer> ids);
    //根据用户id与用户类型查询用户能访问的菜单权限名
    Set<String> getMenuNamesByUserIdAndUserType(Integer userId, Integer userType);
    //根据用户id与用户类型查询用户能访问的目录以及目录下的菜单
    List<MenuVo> getMenuVosByUserIdAndUserType(Integer userId, Integer userType);

    // 获取树形菜单（目录->菜单->按钮）
    List<MenuVo> getTree();
    // 保存或更新菜单
    boolean saveMenu(MenuPojo menu);
    // 删除菜单及其所有子菜单
    boolean deleteMenu(Integer id);
    // 同级移动菜单（direction: up 上移 / down 下移），只交换相邻两个节点的 order_num
    boolean moveMenu(Integer id, String direction);


}
