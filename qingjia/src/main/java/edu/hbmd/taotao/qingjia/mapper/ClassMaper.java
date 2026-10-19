package edu.hbmd.taotao.qingjia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.vo.ClassVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClassMaper extends BaseMapper<ClassPojo> {
    public Page<ClassVo> pageClasses(IPage<?> page,
                                     @Param("clazz") ClassPojo clazz);
    public List<ClassVo> listClasses(@Param("clazz") ClassPojo clazz);

}
