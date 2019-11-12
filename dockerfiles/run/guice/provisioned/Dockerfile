# Run James
#
# VERSION	1.0

FROM james-debug-jpa

# Install git
# RUN apt-get update
# RUN apt-get install -y git --allow-unauthenticated

WORKDIR /root

# RUN git clone https://github.com/vishnubob/wait-for-it.git wait-for-it
# RUN cp /root/wait-for-it/wait-for-it.sh /usr/bin/wait-for-it.sh

COPY wait-for-it.sh /usr/bin

COPY startup.sh /root
COPY initialdata.sh /root

RUN chmod +x /root/startup.sh
RUN chmod +x /root/initialdata.sh

ENTRYPOINT ["./startup.sh"]