package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.mapper.ApplicationStatusMapper;
import edu.hbmd.taotao.qingjia.pojo.ApplicationStatusPojo;
import edu.hbmd.taotao.qingjia.service.ApplicationStatusService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationStatusServiceImpl extends ServiceImpl<ApplicationStatusMapper, ApplicationStatusPojo>
implements ApplicationStatusService {
}
