package edu.hbmd.taotao.qingjia.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("ApplicationStatusPojo")
@TableName("application_status")
public class ApplicationStatusPojo {
    private Integer id;
    private String applicationStutas;
}
