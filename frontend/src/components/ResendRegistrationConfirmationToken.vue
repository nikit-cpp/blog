<template>
    <div class="resend-registration-confirmation-token">
        <template v-if="!emailSuccessfullySent">
            <template v-if="!sending">
                <h1>Resend confirmation email</h1>
                Enter email for resend confirmation
                <input v-model="email"/>
                <error v-show="errors.email" :message="errors.email"></error>
                <button @click="resend()" v-bind:disabled="!submitEnabled" class="blog-btn ok-btn">Resend</button>
            </template>
            <template v-else>
                <blog-spinner message="Sending email..."/>
            </template>
            <error v-show="errors.server" :message="errors.server"></error>
        </template>
        <template v-else>
            <span class="email-successfully-sent" v-if="emailSuccessfullySent">Check your email</span>
        </template>
    </div>
</template>

<script>
    import required from 'vuelidate/lib/validators/required'
    import email from 'vuelidate/lib/validators/email'
    import Error from './Error.vue'
    import BlogSpinner from './BlogSpinner.vue'

    export default {
        data() {
            return {
                errors: {

                },
                email: null,
                emailSuccessfullySent: false,
                submitEnabled: true,
                sending: false,
            }
        },
        created() {
            this.email = this.$route.query.email
        },
        components: {Error, BlogSpinner},
        methods: {
            resend() {
                this.errors.message = null;
                this.emailSuccessfullySent = false;
                this.$data.sending = true;
                this.$http.post('/api/resend-confirmation-email?email=' + this.email).then(response => {
                    this.$data.sending = false;
                    this.emailSuccessfullySent = true;
                }, response => {
                    console.error(response);
                    // alert(response);
                    this.$data.sending = false;
                    this.errors.server = response.body.message;
                });
            },
            validate() {
                this.errors = {};
                this.errors.email = required(this.email) ? false : 'Email is required';
                if (!this.errors.email) { // if previous check is passed
                    this.errors.email = email(this.email) ? false : 'Email is invalid';
                }

                let hasErrors = false;
                Object.keys(this.errors).forEach(item => {
                    hasErrors = hasErrors || !!this.errors[item]; // !! - convert to boolean
                });
                return hasErrors
            },
            updateSubmitEnabled(){
                let hasErrors = this.validate();
                this.submitEnabled = !hasErrors;
            },
        },
        watch: {
            email() {
                this.emailSuccessfullySent = false;
                this.updateSubmitEnabled();
            }
        },
        metaInfo: {
            title: 'Resend confirmation email',
        }
    }
</script>

<style lang="stylus">
    .resend-registration-confirmation-token button {
        display unset
    }
</style>