<script setup>
    import { computed, onUnmounted, reactive, ref, watch } from "vue";

    const props = defineProps({
        apiBase: {
            type: String,
            required: true,
        },
        loading: {
            type: Boolean,
            required: true,
        },
        agreePolicy: {
            type: Boolean,
            required: true,
        },
    });

    const emit = defineEmits([
        "update:loading",
        "update:agree-policy",
        "toast",
        "success",
        "open-agreement",
    ]);

    const registerForm = reactive({
        username: "",
        password: "",
        email: "",
        emailCode: "",
        realName: "",
        role: "USER",
        adminPassword: "",
        phone: "",
    });

    const errors = reactive({
        username: "",
        password: "",
        email: "",
        emailCode: "",
        realName: "",
        adminPassword: "",
        phone: "",
    });

    const touched = reactive({
        email: false,
        phone: false,
        password: false,
    });

    const showPassword = ref(false);
    const showAdminPassword = ref(false);
    const emailCooldown = ref(0);
    const emailTimer = ref(null);

    const roleOptions = [
        { label: "普通用户（USER）", value: "USER" },
        { label: "场馆管理员（OWNER）", value: "OWNER" },
    ];

    const isOwnerRegister = computed(() => registerForm.role === "OWNER");
    const canSubmit = computed(() => !props.loading);
    const isEmailCooldown = computed(() => emailCooldown.value > 0);
    const isEmailValid = computed(() =>
        validateEmail(registerForm.email, false),
    );
    const isPhoneValid = computed(() =>
        validatePhone(registerForm.phone, false),
    );
    const passwordStrength = computed(() =>
        getPasswordStrength(registerForm.password),
    );
    const passwordStrengthLabel = computed(() => {
        if (passwordStrength.value === "strong") return "强";
        if (passwordStrength.value === "medium") return "中";
        return "弱";
    });

    watch(
        () => registerForm.role,
        (role) => {
            if (role !== "OWNER") {
                registerForm.adminPassword = "";
                errors.adminPassword = "";
            }
        },
    );


    onUnmounted(() => {
        if (emailTimer.value) {
            clearInterval(emailTimer.value);
        }
    });

    function setError(field, message) {
        errors[field] = message;
    }

    function resetErrors() {
        errors.username = "";
        errors.password = "";
        errors.email = "";
        errors.emailCode = "";
        errors.realName = "";
        errors.adminPassword = "";
        errors.phone = "";
    }

    function validateEmail(value, updateError = true) {
        const emailValue = value.trim();
        if (!emailValue) {
            if (updateError) setError("email", "请输入邮箱");
            return false;
        }
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(emailValue)) {
            if (updateError) setError("email", "请输入有效邮箱");
            return false;
        }
        if (updateError) setError("email", "");
        return true;
    }

    function validatePhone(value, updateError = true) {
        const phoneValue = value.trim();
        if (!phoneValue) {
            if (updateError) setError("phone", "");
            return true;
        }
        const phonePattern = /^1[3-9]\d{9}$/;
        if (!phonePattern.test(phoneValue)) {
            if (updateError) setError("phone", "请输入有效手机号");
            return false;
        }
        if (updateError) setError("phone", "");
        return true;
    }

    function handleEmailBlur() {
        touched.email = true;
        validateEmail(registerForm.email, true);
    }

    function handlePhoneBlur() {
        touched.phone = true;
        validatePhone(registerForm.phone, true);
    }

    function handlePasswordBlur() {
        touched.password = true;
        if (!registerForm.password.trim()) {
            setError("password", "请输入密码");
        } else if (passwordStrength.value === "weak") {
            setError("password", "密码强度太弱");
        } else {
            setError("password", "");
        }
    }

    function getPasswordStrength(value) {
        const passwordValue = value.trim();
        if (!passwordValue) return "weak";

        let score = 0;
        if (passwordValue.length >= 8) score += 1;
        if (/[A-Z]/.test(passwordValue)) score += 1;
        if (/[a-z]/.test(passwordValue)) score += 1;
        if (/[0-9]/.test(passwordValue)) score += 1;
        if (/[^A-Za-z0-9]/.test(passwordValue)) score += 1;

        if (score >= 4) return "strong";
        if (score >= 2) return "medium";
        return "weak";
    }

    function validate() {
        resetErrors();
        let valid = true;
        if (!registerForm.username.trim()) {
            setError("username", "请输入用户名");
            valid = false;
        }
        if (!registerForm.password.trim()) {
            setError("password", "请输入密码");
            valid = false;
        } else if (passwordStrength.value === "weak") {
            setError("password", "密码强度太弱");
            valid = false;
        }
        if (!validateEmail(registerForm.email, true)) {
            valid = false;
        }
        if (!registerForm.emailCode.trim()) {
            setError("emailCode", "请输入验证码");
            valid = false;
        }
        if (!registerForm.realName.trim()) {
            setError("realName", "请输入真实姓名");
            valid = false;
        }
        if (!validatePhone(registerForm.phone, true)) {
            valid = false;
        }
        if (
            registerForm.role === "OWNER" &&
            !registerForm.adminPassword.trim()
        ) {
            setError("adminPassword", "OWNER 注册需要管理员密码");
            valid = false;
        }
        if (!props.agreePolicy) {
            emit("toast", "warning", "请先同意用户协议");
            valid = false;
        }
        return valid;
    }

    async function handleSendEmailCode() {
        if (
            !validateEmail(registerForm.email, true) ||
            isEmailCooldown.value ||
            props.loading
        )
            return;
        emit("update:loading", true);
        try {
            const response = await fetch(`${props.apiBase}/auth/email/code`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ email: registerForm.email }),
            });
            const result = await response.json();
            if (result.code !== 200) {
                emit("toast", "error", result.message || "验证码发送失败");
                return;
            }
            emit("toast", "success", "验证码已发送");
            emailCooldown.value = 60;
            if (emailTimer.value) clearInterval(emailTimer.value);
            emailTimer.value = setInterval(() => {
                emailCooldown.value -= 1;
                if (emailCooldown.value <= 0) {
                    clearInterval(emailTimer.value);
                    emailTimer.value = null;
                }
            }, 1000);
        } catch (error) {
            emit("toast", "error", "无法连接后端服务");
        } finally {
            emit("update:loading", false);
        }
    }

    async function handleRegister() {
        if (!validate() || !canSubmit.value) return;
        emit("update:loading", true);
        try {
            const payload = {
                username: registerForm.username,
                password: registerForm.password,
                email: registerForm.email,
                emailCode: registerForm.emailCode,
                realName: registerForm.realName,
                role: registerForm.role,
                adminPassword:
                    registerForm.role === "OWNER"
                        ? registerForm.adminPassword
                        : "",
                phone: registerForm.phone,
            };
            const response = await fetch(`${props.apiBase}/auth/register`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(payload),
            });
            const result = await response.json();
            if (result.code !== 200) {
                emit("toast", "error", result.message || "注册失败");
                return;
            }
            emit("success");
        } catch (error) {
            emit("toast", "error", "无法连接后端服务");
        } finally {
            emit("update:loading", false);
        }
    }
</script>

<template>
    <form
        class="form"
        @submit.prevent="handleRegister"
    >
        <div class="field">
            <label for="register-username">用户名</label>
            <input
                id="register-username"
                v-model="registerForm.username"
                placeholder="请输入用户名"
                :class="{ error: errors.username }"
            />
            <span
                v-if="errors.username"
                class="error-text"
                >{{ errors.username }}</span
            >
        </div>

        <div class="field">
            <label for="register-password">
                密码
                <span
                    v-if="touched.password"
                    class="strength-pill inline"
                    :class="passwordStrength"
                >
                    {{ passwordStrengthLabel }}
                </span>
            </label>
            <div class="password-field">
                <input
                    id="register-password"
                    v-model="registerForm.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="请输入密码"
                    :class="{
                        error: errors.password,
                        valid: touched.password && passwordStrength === 'strong',
                    }"
                    @blur="handlePasswordBlur"
                />
                <button
                    type="button"
                    class="ghost"
                    @click="showPassword = !showPassword"
                >
                    {{ showPassword ? "隐藏" : "显示" }}
                </button>
            </div>
        </div>

        <div class="field">
            <label for="register-email">邮箱</label>
            <div class="password-field">
                <input
                    id="register-email"
                    v-model="registerForm.email"
                    placeholder="请输入邮箱"
                    :class="{
                        error: errors.email,
                        valid: touched.email && isEmailValid,
                    }"
                    @blur="handleEmailBlur"
                />
                <button
                    type="button"
                    class="ghost"
                    :disabled="isEmailCooldown || loading"
                    @click="handleSendEmailCode"
                >
                    {{ isEmailCooldown ? `${emailCooldown}s` : "发送验证码" }}
                </button>
            </div>
            <span
                v-if="errors.email"
                class="error-text"
                >{{ errors.email }}</span
            >
        </div>

        <div class="field">
            <label for="register-email-code">邮箱验证码</label>
            <input
                id="register-email-code"
                v-model="registerForm.emailCode"
                placeholder="请输入邮箱验证码"
                :class="{ error: errors.emailCode }"
            />
            <span
                v-if="errors.emailCode"
                class="error-text"
                >{{ errors.emailCode }}</span
            >
        </div>

        <div class="field">
            <label for="register-realname">真实姓名</label>
            <input
                id="register-realname"
                v-model="registerForm.realName"
                placeholder="请输入真实姓名"
                :class="{ error: errors.realName }"
            />
            <span
                v-if="errors.realName"
                class="error-text"
                >{{ errors.realName }}</span
            >
        </div>

        <div class="field">
            <label for="register-role">注册角色</label>
            <select
                id="register-role"
                v-model="registerForm.role"
            >
                <option
                    v-for="role in roleOptions"
                    :key="role.value"
                    :value="role.value"
                >
                    {{ role.label }}
                </option>
            </select>
        </div>

        <div
            v-if="isOwnerRegister"
            class="field"
        >
            <label for="register-admin">管理员密码</label>
            <div class="password-field">
                <input
                    id="register-admin"
                    v-model="registerForm.adminPassword"
                    :type="showAdminPassword ? 'text' : 'password'"
                    placeholder="请输入 ADMIN 密码"
                    :class="{ error: errors.adminPassword }"
                />
                <button
                    type="button"
                    class="ghost"
                    @click="showAdminPassword = !showAdminPassword"
                >
                    {{ showAdminPassword ? "隐藏" : "显示" }}
                </button>
            </div>
            <span
                v-if="errors.adminPassword"
                class="error-text"
                >{{ errors.adminPassword }}</span
            >
        </div>

        <div class="field">
            <label for="register-phone">手机号</label>
            <input
                id="register-phone"
                v-model="registerForm.phone"
                placeholder="可选"
                :class="{
                    error: errors.phone,
                    valid: touched.phone && isPhoneValid,
                }"
                @blur="handlePhoneBlur"
            />
            <span
                v-if="errors.phone"
                class="error-text"
                >{{ errors.phone }}</span
            >
        </div>

        <div class="form-actions">
            <label class="checkbox">
                <input
                    type="checkbox"
                    :checked="agreePolicy"
                    @change="emit('update:agree-policy', $event.target.checked)"
                />
                同意
                <button
                    type="button"
                    class="inline-link"
                    @click="emit('open-agreement')"
                >
                    用户协议与隐私政策
                </button>
            </label>
            <span class="text-muted">注册即代表同意平台条款</span>
        </div>

        <button
            class="primary"
            type="submit"
            :disabled="loading"
        >
            {{ loading ? "提交中..." : "创建账号" }}
        </button>
    </form>
</template>

<style scoped>
.inline-link {
    border: none;
    background: transparent;
    color: #2563eb;
    padding: 0;
    margin: 0;
    font: inherit;
    cursor: pointer;
    text-decoration: underline;
}

.inline-link:hover {
    color: #1d4ed8;
}
</style>


