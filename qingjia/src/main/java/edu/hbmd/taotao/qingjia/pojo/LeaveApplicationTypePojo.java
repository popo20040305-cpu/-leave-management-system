package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("LeaveApplicationTypePojo")
@TableName("leave_application_type")
public class LeaveApplicationTypePojo {
    @TableId(type = IdType.AUTO)
    private Integer  id;
    private String  name;
}
