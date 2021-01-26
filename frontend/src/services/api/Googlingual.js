import axios from 'axios'

const BASE_URL='https://googlingual-api-service-dot-gcloud-dpe.ue.r.appspot.com';

export default {
  async translate(message) {
    return axios.post(`${BASE_URL}/translate`, message, {
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
  async send(message) {
    return axios.post(`${BASE_URL}/v1/send`, message, {
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
    return axios.get(`${BASE_URL}/v1/locales`, {
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
    return axios.get(`${BASE_URL}/v1/audioLocales/${lang}`, {
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
