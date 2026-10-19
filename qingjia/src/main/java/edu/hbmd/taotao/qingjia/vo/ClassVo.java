package edu.hbmd.taotao.qingjia.vo;

import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("ClassVo")
public class ClassVo extends ClassPojo {
    private String bzrName;
    private String collegeName;
}

