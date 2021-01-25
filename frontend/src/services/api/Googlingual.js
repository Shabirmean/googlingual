import axios from 'axios'

const BASE_URL='https://googlingual-api-service-dot-gcloud-dpe.ue.r.appspot.com';
// axios.defaults.baseUrl = 'http://googlingual-api-service-dot-gcloud-dpe.ue.r.appspot.com'

export default {
  translate(message) {
    return axios.post(`${BASE_URL}/translate`, message, {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Content-Type': 'application/json'
      }
    });
  },
  send(message) {
    return axios.post(`${BASE_URL}/v1/send`, message, {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Content-Type': 'application/json'
      }
    });
  },
  locales() {
    return axios.get(`${BASE_URL}/v1/locales`);
  },
  audioLocales(lang) {
    return axios.get(`${BASE_URL}/v1/audioLocales/${lang}`);
  }
}
