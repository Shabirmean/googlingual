import axios from 'axios'
import store from '@/main'

const API_SERVER_URL = (process.env.VUE_APP_API_SERVER_URL) ?
  process.env.VUE_APP_API_SERVER_URL : 'http://localhost:8082';
const HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
  'Content-Type': 'application/json'
};

export default {
  async send(message, isPing = false) {
    const requestHeaders = {
      ...HEADERS,
      'Authorization': `Bearer ${store.getters.accessToken}`,
    };
    if (isPing) {
      requestHeaders['Ping'] = 'keep-alive';
    }
    return axios.post(`${API_SERVER_URL}/send`, message, { headers: requestHeaders })
      .catch((error) => {
        if (!error.response) {
          return { status: 500 };
        }
      });
  },
  async locales() {
    console.log(store.getters.accessToken);
    return axios.get(`${API_SERVER_URL}/locales`, {
      headers: {
        ...HEADERS,
        'Authorization': `Bearer ${store.getters.accessToken}`,
      }
    }).catch((error) => {
      console.log({ ...error })
      if (!error.response) {
        return { status: 500 };
      }
    });
  },
  async audioLocales(lang) {
    return axios.get(`${API_SERVER_URL}/audioLocales/${lang}`, {
      headers: {
        ...HEADERS,
        'Authorization': `Bearer ${store.getters.accessToken}`,
      }
    }).catch((error) => {
      if (!error.response) {
        return { status: 500 };
      }
    });
  }
}
