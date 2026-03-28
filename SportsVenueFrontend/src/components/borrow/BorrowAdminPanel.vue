<script setup>
    import { computed, reactive, watch } from "vue";
    import { useQuery, useQueryClient } from "@tanstack/vue-query";
    import { NButton, NInput, NInputNumber, NModal, NTag } from "naive-ui";
    import api from "../../services/api";
    import { useToast } from "../../composables/useToast";

    const { pushToast } = useToast();
    const queryClient = useQueryClient();

    const adminItemModal = reactive({
        show: false,
        editingId: null,
        submitting: false,
        form: {
            name: "",
            type: "",
            model: "",
            totalQuantity: 0,
            availableQuantity: 0,
            damagedQuantity: 0,
            depositAmount: 0,
            description: "",
        },
    });

    const detailModal = reactive({
        show: false,
        item: null,
    });

    const adminPagination = reactive({ pageNo: 1, pageSize: 10 });

    watch(
        () => adminPagination.pageSize,
        (value, oldValue) => {
            if (value === oldValue) return;
            if (!Number.isFinite(value) || value <= 0) {
                adminPagination.pageSize = oldValue || 10;
                return;
            }
            adminPagination.pageSize = Math.min(50, Math.max(1, Math.floor(value)));
            adminPagination.pageNo = 1;
        },
    );

    const adminItemsQuery = useQuery({
        queryKey: computed(() => [
            "items",
            "admin",
            adminPagination.pageNo,
            adminPagination.pageSize,
        ]),
        queryFn: async () => {
            const response = await api.get("/items", {
                params: {
                    pageNo: adminPagination.pageNo,
                    pageSize: adminPagination.pageSize,
                },
            });
            if (response.code !== 200)
                throw new Error(response.message || "器材加载失败");
            return response.data || { records: [], total: 0 };
        },
        keepPreviousData: true,
        staleTime: 30000,
    });

    const adminItems = computed(
        () =>
            adminItemsQuery.data?.records ||
            adminItemsQuery.data?.value?.records ||
            [],
    );
    const adminTotal = computed(
        () =>
            adminItemsQuery.data?.total ||
            adminItemsQuery.data?.value?.total ||
            0,
    );

    function resetForm() {
        adminItemModal.form = {
            name: "",
            type: "",
            model: "",
            totalQuantity: 0,
            availableQuantity: 0,
            damagedQuantity: 0,
            depositAmount: 0,
            description: "",
        };
    }

    function openAdminCreate() {
        adminItemModal.show = true;
        adminItemModal.editingId = null;
        resetForm();
    }

    function openAdminEdit(item) {
        adminItemModal.show = true;
        adminItemModal.editingId = item.id;
        adminItemModal.form = {
            name: item.name || "",
            type: item.type || "",
            model: item.model || "",
            totalQuantity: item.totalQuantity || 0,
            availableQuantity: item.availableQuantity || 0,
            damagedQuantity: item.damagedQuantity || 0,
            depositAmount: item.depositAmount || 0,
            description: item.description || "",
        };
    }

    function closeAdminModal() {
        adminItemModal.show = false;
    }

    function openDetailModal(item) {
        detailModal.show = true;
        detailModal.item = item;
    }

    function closeDetailModal() {
        detailModal.show = false;
        detailModal.item = null;
    }

    async function submitAdminItem() {
        if (!adminItemModal.form.name?.trim()) {
            pushToast("请填写器材名称", "warning");
            return;
        }
        adminItemModal.submitting = true;
        try {
            const payload = { ...adminItemModal.form };
            const response = adminItemModal.editingId
                ? await api.put(`/items/${adminItemModal.editingId}`, {
                      ...payload,
                      id: adminItemModal.editingId,
                  })
                : await api.post("/items", payload);

            if (response.code !== 200) {
                pushToast(response.message || "提交失败", "error");
                return;
            }

            pushToast(
                adminItemModal.editingId ? "器材信息已更新" : "器材已新增",
                "success",
            );
            closeAdminModal();
            queryClient.invalidateQueries({ queryKey: ["items"] });
        } catch {
            pushToast("提交失败，请稍后重试", "error");
        } finally {
            adminItemModal.submitting = false;
        }
    }

    defineExpose({ openAdminCreate, openAdminEdit });
</script>

<template>
    <section class="card borrow-panel">
        <div class="borrow-panel__header">
            <div>
                <p class="section-kicker">器材管理</p>
                <h3>维护器材台账与库存信息</h3>
            </div>
            <div class="borrow-panel__actions">
                <NButton
                    type="primary"
                    @click="openAdminCreate"
                    >新增器材</NButton
                >
            </div>
        </div>

        <div class="borrow-grid">
            <template v-if="adminItems.length || adminItemsQuery.isFetching">
                <article
                    v-for="item in adminItems"
                    :key="item.id"
                    class="borrow-card"
                >
                    <div class="borrow-card__header">
                        <div class="borrow-card__title-wrap">
                            <h3 :title="item.name">{{ item.name }}</h3>
                            <p class="text-muted" :title="`${item.type || '未分类'} · ${item.model || '无型号'}`">
                                {{ item.type || "未分类" }} ·
                                {{ item.model || "无型号" }}
                            </p>
                        </div>
                        <NTag type="info"
                            >押金 ¥{{ item.depositAmount || 0 }}</NTag
                        >
                    </div>
                    <div class="borrow-card__meta">
                        <div>
                            <span>库存</span
                            ><strong>{{ item.totalQuantity }}</strong>
                        </div>
                        <div>
                            <span>可借</span
                            ><strong>{{ item.availableQuantity }}</strong>
                        </div>
                        <div>
                            <span>损坏</span
                            ><strong>{{ item.damagedQuantity || 0 }}</strong>
                        </div>
                    </div>
                    <p class="borrow-card__desc" :title="item.description || '暂无描述'">
                        {{ item.description || "暂无描述" }}
                    </p>
                    <div class="borrow-card__actions">
                        <NButton size="small" @click="openDetailModal(item)">详细信息</NButton>
                        <NButton
                            size="small"
                            @click="openAdminEdit(item)"
                            >编辑</NButton
                        >
                    </div>
                </article>
            </template>

            <div
                v-else
                class="empty-state"
            >
                <h3>暂无器材数据</h3>
                <p>点击“新增器材”创建第一条器材台账。</p>
            </div>
        </div>

        <section class="pagination">
            <NButton
                tertiary
                @click="
                    adminPagination.pageNo = Math.max(
                        1,
                        adminPagination.pageNo - 1,
                    );
                    adminItemsQuery.refetch();
                "
                :disabled="adminPagination.pageNo <= 1"
                >上一页</NButton
            >
            <span
                >第 {{ adminPagination.pageNo }} 页 / 共
                {{
                    Math.ceil(adminTotal / adminPagination.pageSize) || 1
                }}
                页</span
            >
            <span
                style="
                    display: inline-flex;
                    align-items: center;
                    gap: 8px;
                "
            >
                <span>每页</span>
                <NInputNumber
                    v-model:value="adminPagination.pageSize"
                    :min="1"
                    :max="50"
                    :step="1"
                    style="width: 100px"
                />
                <span>条</span>
            </span>
            <NButton
                tertiary
                @click="
                    adminPagination.pageNo += 1;
                    adminItemsQuery.refetch();
                "
                :disabled="
                    adminPagination.pageNo * adminPagination.pageSize >=
                    adminTotal
                "
                >下一页</NButton
            >
        </section>
    </section>

    <NModal
        v-model:show="adminItemModal.show"
        preset="card"
        class="booking-modal"
        title="器材台账管理"
    >
        <div class="booking-modal__section">
            <label>器材名称</label
            ><NInput v-model:value="adminItemModal.form.name" />
        </div>
        <div class="booking-modal__section">
            <label>器材类型</label
            ><NInput v-model:value="adminItemModal.form.type" />
        </div>
        <div class="booking-modal__section">
            <label>型号</label
            ><NInput v-model:value="adminItemModal.form.model" />
        </div>
        <div class="booking-modal__section two-col">
            <div>
                <label>库存总量</label
                ><NInputNumber
                    v-model:value="adminItemModal.form.totalQuantity"
                    :min="0"
                />
            </div>
            <div>
                <label>可借数量</label
                ><NInputNumber
                    v-model:value="adminItemModal.form.availableQuantity"
                    :min="0"
                />
            </div>
        </div>
        <div class="booking-modal__section two-col">
            <div>
                <label>损坏数量</label
                ><NInputNumber
                    v-model:value="adminItemModal.form.damagedQuantity"
                    :min="0"
                />
            </div>
            <div>
                <label>押金金额</label
                ><NInputNumber
                    v-model:value="adminItemModal.form.depositAmount"
                    :min="0"
                />
            </div>
        </div>
        <div class="booking-modal__section">
            <label>描述</label
            ><NInput
                v-model:value="adminItemModal.form.description"
                type="textarea"
            />
        </div>
        <div class="booking-modal__actions">
            <NButton @click="closeAdminModal">取消</NButton>
            <NButton
                type="primary"
                :loading="adminItemModal.submitting"
                @click="submitAdminItem"
                >{{
                    adminItemModal.editingId ? "保存修改" : "确认新增"
                }}</NButton
            >
        </div>
    </NModal>

    <NModal
        v-model:show="detailModal.show"
        preset="card"
        class="booking-modal"
        title="器材详情"
        @close="closeDetailModal"
    >
        <div class="borrow-detail">
            <div class="borrow-detail__header">
                <div>
                    <h3>{{ detailModal.item?.name || '—' }}</h3>
                    <p class="text-muted">{{ detailModal.item?.type || '未分类' }} · {{ detailModal.item?.model || '无型号' }}</p>
                </div>
                <NTag type="info">押金 ¥{{ detailModal.item?.depositAmount || 0 }}</NTag>
            </div>
            <div class="borrow-detail__grid">
                <div><span>库存总量</span><strong>{{ detailModal.item?.totalQuantity ?? 0 }}</strong></div>
                <div><span>可借数量</span><strong>{{ detailModal.item?.availableQuantity ?? 0 }}</strong></div>
                <div><span>损坏数量</span><strong>{{ detailModal.item?.damagedQuantity ?? 0 }}</strong></div>
            </div>
            <div class="borrow-detail__desc">
                <label>描述</label>
                <p>{{ detailModal.item?.description || '暂无描述' }}</p>
            </div>
        </div>
        <div class="booking-modal__actions">
            <NButton type="primary" @click="closeDetailModal">关闭</NButton>
        </div>
    </NModal>
</template>

<style scoped>
    .borrow-detail {
        display: flex;
        flex-direction: column;
        gap: 16px;
    }

    .borrow-detail__header {
        display: flex;
        justify-content: space-between;
        gap: 16px;
        align-items: center;
    }

    .borrow-detail__grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 12px;
    }

    .borrow-detail__grid span {
        display: block;
        font-size: 12px;
        color: #7b8aa6;
    }

    .borrow-detail__grid strong {
        font-size: 16px;
        color: #1c2a44;
    }

    .borrow-detail__desc label {
        display: block;
        font-size: 12px;
        color: #7b8aa6;
        margin-bottom: 6px;
    }

    .borrow-detail__desc p {
        margin: 0;
        color: #1c2a44;
        line-height: 1.6;
        word-break: break-word;
    }
</style>
