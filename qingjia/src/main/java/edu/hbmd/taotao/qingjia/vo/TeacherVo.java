package edu.hbmd.taotao.qingjia.vo;

import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.Alias;

@Data
@EqualsAndHashCode(callSuper = true)
@Alias("TeacherVo")
public class TeacherVo extends TeacherPojo {
    private String collegeName;
}
