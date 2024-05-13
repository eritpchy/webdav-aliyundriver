FROM ubuntu:22.04 as builder
# Allow ubuntu to cache package downloads
RUN rm -f /etc/apt/apt.conf.d/docker-clean
RUN --mount=type=cache,target=/var/cache/apt,sharing=private \
    apt update \
    && DEBIAN_FRONTEND=noninteractive apt install -y curl build-essential libz-dev zlib1g-dev
RUN curl -sL https://get.graalvm.org/jdk | bash -s --
ENV LANG=C.UTF-8
RUN cd /graalvm* \
    && GRAALVM_PATH=$(pwd)  \
    && echo $GRAALVM_PATH \
    && echo "export PATH=\"$GRAALVM_PATH/bin:\$PATH\"" >> /tmp/env \
    && echo "export JAVA_HOME=\"$GRAALVM_PATH\"" >> /tmp/env
COPY ./build.gradle /tmp/webdav-aliyundriver/build.gradle
COPY ./gradle /tmp/webdav-aliyundriver/gradle
COPY ./gradlew /tmp/webdav-aliyundriver/gradlew
COPY ./settings.gradle /tmp/webdav-aliyundriver/settings.gradle
COPY ./gradle.properties /tmp/webdav-aliyundriver/gradle.properties
RUN --mount=type=cache,target=/root/.gradle,sharing=private \
    . /tmp/env && cd /tmp/webdav-aliyundriver && ./gradlew --info dependencies
COPY ./ /tmp/webdav-aliyundriver
RUN --mount=type=cache,target=/root/.gradle,sharing=private \
    . /tmp/env \
    && cd /tmp/webdav-aliyundriver \
    && ./gradlew nativeCompile --no-daemon
RUN chmod +x /tmp/webdav-aliyundriver/build/native/nativeCompile/webdav-aliyundriver

FROM ubuntu:22.04
COPY --from=builder /tmp/webdav-aliyundriver/build/native/nativeCompile/webdav-aliyundriver /webdav-aliyundriver
ENV LANG=C.UTF-8
EXPOSE 8080
ENTRYPOINT ["/webdav-aliyundriver"]
