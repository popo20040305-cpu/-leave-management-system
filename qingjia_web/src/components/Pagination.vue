<template>
    <div v-if="!autoHide || total > 0" class="pagination-bar" :style="{ justifyContent: justify }">
        <el-pagination :current-page="current" :page-size="size" :total="total" :page-sizes="pageSizes"
            :layout="layout" :background="background" @current-change="handleCurrentChange"
            @size-change="handleSizeChange" />
    </div>
</template>

<script setup lang="ts">
/**
 * 公共分页组件，包一层 el-pagination，统一全站列表页的分页外观和行为。
 *
 * 用法：
 *   <Pagination v-model:current="page.current" v-model:size="page.size"
 *               :total="page.total" @change="page.onPageChange" />
 * 配合 usePageTable() 用时上面这些字段都是现成的，直接绑即可。
 *
 * 为什么不直接在页面里写 el-pagination：
 * 1. 每页条数变化时 el-pagination 会先抛 size-change；如果当前页超出了新的总页数，
 *    它紧接着还会自己再抛一次 current-change。不拦一下，父组件的 change 会触发两次、
 *    列表请求打两遍（第一遍还是用旧页码打的，结果被第二遍覆盖，表格白白闪一下）。
 *    下面那个 suppressCurrentChange 闸门就是干这个的。
 * 2. layout / background / 每页条数可选项这些全站应当一致，收在一处好改。
 */
import { computed, nextTick } from 'vue'
import { DEFAULT_PAGE_SIZES } from '@/type/page'
import { setPageSize } from '@/api/pageSizeApi'

const props = withDefaults(
    defineProps<{
        /** 当前页码，从 1 开始 */
        current: number
        /** 每页条数 */
        size: number
        /** 总记录数 */
        total: number
        /** 每页条数下拉可选项 */
        pageSizes?: number[]
        layout?: string
        background?: boolean
        /** total 为 0 时整条隐藏，避免空列表下面孤零零挂一个“共 0 条” */
        autoHide?: boolean
        align?: 'left' | 'center' | 'right'
    }>(),
    {
        pageSizes: () => DEFAULT_PAGE_SIZES,
        layout: 'total, sizes, prev, pager, next, jumper',
        background: true,
        autoHide: false,
        align: 'right',
    },
)

const emit = defineEmits<{
    (e: 'update:current', value: number): void
    (e: 'update:size', value: number): void
    /** 页码或每页条数变化时触发一次（只一次），父组件在这里重新拉列表 */
    (e: 'change', payload: { current: number; size: number }): void
}>()

const justify = computed(() => {
    if (props.align === 'left') return 'flex-start'
    if (props.align === 'center') return 'center'
    return 'flex-end'
})

// 见文件头注释第 1 点
let suppressCurrentChange = false

const handleSizeChange = (value: number) => {
    if (value === props.size) return
    suppressCurrentChange = true
    // 记住这次选择（本地 + 后端 session，见 pageSizeApi.ts）。
    // fire-and-forget：setPageSize 内部已经吞掉失败，列表刷新不该被它拖着
    void setPageSize(value)
    emit('update:size', value)
    // 换了每页条数一律回到第一页，否则很容易停在一个已经不存在的页码上
    emit('update:current', 1)
    emit('change', { current: 1, size: value })
    nextTick(() => {
        suppressCurrentChange = false
    })
}

const handleCurrentChange = (value: number) => {
    if (suppressCurrentChange) return
    if (value === props.current) return
    emit('update:current', value)
    emit('change', { current: value, size: props.size })
}
</script>

<style scoped>
.pagination-bar {
    display: flex;
    width: 100%;
    padding: 16px 0;
}
</style>
