#this scripts path is /etc/init.d
#!/bin/sh
sudo killall uv4l
uv4l --driver raspicam --config-file /etc/uv4l/uv4l-raspicam.conf --auto-video_nr --nopreview
sleep 3
curl --insecure -s 'http://127.0.0.1:8080/janus?gateway_url=<EC2 IP>&gateway_root=/janus&room=1234&room_pin=&username=&token=&publish=1&subscribe=0&hw_vcodec=0&vformat=60&reconnect=1&proxy_host=&proxy_port=80&proxy_password=&proxy_bypass=&reconnect=1&action=Start&' > /dev/null
sleep 3
curl --insecure -s 'http://127.0.0.1:8080/panel?width=640&height=480&format=842093913&9963776=50&9963777=0&9963778=0&9963790=100&9963791=100&9963803=0&9963810=0&134217728=0&134217729=1&134217730=0&134217739=85&134217741=30&9963796=1&9963797=1&134217734=0&134217736=0&134217737=1&134217738=0&134217740=0&134217731=0&134217732=1&134217733=0&134217735=3&apply_changed=1' > /dev/null
sudo killall pigpiod
sudo pigpiod
sudo python <iot python script> -e <iot-end point> -r root-CA.crt -c smartcar.cert.pem -k smartcar.private.key
echo "---------------- Camera Init Done!--------------"
