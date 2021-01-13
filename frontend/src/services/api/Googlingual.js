import axios from 'axios'

const BASE_URL='http://googlingual-api-service-dot-gcloud-dpe.ue.r.appspot.com';
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
  }
}
