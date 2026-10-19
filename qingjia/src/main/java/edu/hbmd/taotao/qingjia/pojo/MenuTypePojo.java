package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("MenuTypePojo")
@TableName("menu_type")
public class MenuTypePojo {
    private Integer  id;
    private String  name;
}
