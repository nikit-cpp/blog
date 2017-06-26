// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueResource from 'vue-resource'
import App from './App.vue'
import router from './router.js'
// import {login} from './router.js'
import vmodal from 'vue-js-modal'
import VeeValidate from 'vee-validate';

Vue.use(VueResource);
Vue.use(vmodal);
Vue.use(VeeValidate);

Vue.config.devtools = false;

Vue.http.interceptors.push((request, next)  => {
    next((response) => {
        if(response.status === 401 ) {
            // logout();

            // не думал что такое будет работать ;)
            vm.$modal.show('demo-login');
        }
    });
});


/* eslint-disable no-new */
const vm = new Vue({
  el: '#app-container',
  router,
  template: '<App/>',
  components: { App }
});

