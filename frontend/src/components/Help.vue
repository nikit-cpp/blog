<template>
    <ul class="help">
        <li><router-link class="router-link" to="/registration-confirmation-resend">Didn't receive registration confirmation ?</router-link></li>
        <li><router-link class="router-link" to="/password-restore">Forgot password?</router-link></li>
    </ul>
</template>

<script>
    export default {
        metaInfo: {
            title: 'Help',
        }
    }
</script>