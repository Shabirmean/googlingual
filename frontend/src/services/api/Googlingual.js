import axios from 'axios'

const API_SERVER_URL = (process.env.VUE_APP_API_SERVER_URL) ?
  process.env.VUE_APP_API_SERVER_URL : 'http://localhost:8082';

export default {
  async send(message) {
    return axios.post(`${API_SERVER_URL}/send`, message, {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Content-Type': 'application/json'
      }
    }).catch((error) => {
      if (!error.response) {
        return { status: 500 };
      }
    });
  },
  async locales() {
    return axios.get(`${API_SERVER_URL}/locales`, {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Content-Type': 'application/json'
      }
    }).catch((error) => {
      if (!error.response) {
        return { status: 500 };
      }
    });
  },
  async audioLocales(lang) {
    return axios.get(`${API_SERVER_URL}/audioLocales/${lang}`, {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Content-Type': 'application/json'
      }
    }).catch((error) => {
      if (!error.response) {
        return { status: 500 };
      }
    });
  }
}
