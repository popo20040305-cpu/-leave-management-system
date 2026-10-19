package edu.hbmd.taotao.qingjia.vo;

import edu.hbmd.taotao.qingjia.pojo.MenuPojo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class MenuVo extends MenuPojo {
    private List<MenuVo> subMenus = new ArrayList<>();

    public MenuVo(MenuPojo menuPojo) {
        this.setId(menuPojo.getId());
        this.setName(menuPojo.getName());
        this.setPermission(menuPojo.getPermission());
        this.setPid(menuPojo.getPid());
        this.setPids(menuPojo.getPids());
        this.setUrl(menuPojo.getUrl());
        this.setType(menuPojo.getType());
        this.setIcon(menuPojo.getIcon());
        this.setOrder_num(menuPojo.getOrder_num());
    }

    public void addSubMenu(MenuPojo menuPojo){
        MenuVo menuVo = new MenuVo(menuPojo);
        this.subMenus.add(menuVo);

    }
}
