package edu.hbmd.taotao.qingjia.vo;

import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("StudentVo")
public class StudentVo  extends StudentPojo {
    private String className;//班级名
    private Integer grade;//年级
    private Integer collegeId;//学院id
    private String collegeName;//学院名
}
