#include <jni.h>

#include <NeedlePositionGenerator.h>

extern "C" {

JNIEXPORT void JNICALL Java_com_testapp_speedometer_service_GenerateService_00024Companion_initGenerateNeedleMotion(
	JNIEnv * env,
	[[maybe_unused]] jobject thiz,
	jobject needle_position,
	jdouble max_speed)
{
	auto & needle_generator = generate_service::NeedlePositionGenerator::Instance();
	needle_generator.set_enviroment(env, needle_position, max_speed);
}

JNIEXPORT void JNICALL Java_com_testapp_speedometer_service_GenerateService_00024Companion_startGenerateNeedleMotion(
	[[maybe_unused]] JNIEnv * env,
	[[maybe_unused]] jobject thiz)
{
	generate_service::NeedlePositionGenerator::Instance().start_generate();
}

JNIEXPORT void JNICALL Java_com_testapp_speedometer_service_GenerateService_00024Companion_stopGenerateNeedleMotion(
	[[maybe_unused]] JNIEnv * env,
	[[maybe_unused]] jobject thiz)
{
	generate_service::NeedlePositionGenerator::Instance().stop_generate();
}
}
