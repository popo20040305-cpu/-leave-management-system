package edu.hbmd.taotao.qingjia.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component("uploadFileProperties")
@ConfigurationProperties(prefix = "project.upload")
public class UploadFileProperties {
    private String uploadFileTeacherPath;//教师图像文件上传目录
    private String uploadFileStudentPath;//学生图像文件上传目录
    private String uploadFileXiaoJiaPath;//学生销假照片文件上传目录
}
