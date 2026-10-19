package edu.hbmd.taotao.qingjia;

import edu.hbmd.taotao.qingjia.util.PasswordUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QingjiaApplication {

    public static void main(String[] args) {
        // 在java  VM 也就是java 虚拟机中配置系统属性
        System.setProperty("log.dir", "D:/myapp/logs");

        SpringApplication.run(QingjiaApplication.class, args);
    }
}
